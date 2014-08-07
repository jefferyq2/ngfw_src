import os
import sys
import subprocess
import time
import datetime

class ClientControl:

    # global variables
    hostIP = "192.168.2.100"
    hostUsername = "testshell"
    hostKeyFile = "/usr/lib/python2.7/untangle_tests/testShell.key"
    logfile = None
    verbosity = 0
    sshOptions = "-o StrictHostKeyChecking=no -o ConnectTimeout=300 -o ConnectionAttempts=15"
    quickTestsOnly = False
    interface = 0
    interfaceExternal = 0

    # set the key file permissions correctly just in case
    os.system("chmod 600 %s" % hostKeyFile)

    def redirectOutput(self, logfile):
        self.orig_stdout = sys.stdout
        self.orig_stderr = sys.stderr
        sys.stdout = logfile
        sys.stderr = logfile

    def restoreOutput(self):
        sys.stdout = self.orig_stdout
        sys.stderr = self.orig_stderr

    # runs a given command
    # returns 0 if the process returned 0, 1 otherwise
    def runCommand(self, command, stdout=False, nowait=False):

        if not stdout:
            shellRedirect = " >/dev/null 2>&1 "
        else:
            shellRedirect = ""

        if (ClientControl.logfile != None):
            self.redirectOutput(ClientControl.logfile)

        result = 1
        try:
            sshCommand = "ssh %s -i %s %s@%s \"%s %s\" %s" % (ClientControl.sshOptions, ClientControl.hostKeyFile, ClientControl.hostUsername, ClientControl.hostIP, command, shellRedirect, shellRedirect)
            if (ClientControl.verbosity > 1):
                print "\nRunning command          : %s" % sshCommand
            if (ClientControl.verbosity > 0):
                print "\nRunning command on client: %s" % command
            if (nowait):
                sshCommand += " & " # don't wait for process to complete
            if (not stdout):
                result = os.system(sshCommand)
                # If nowait, sleep for a second to give time for the ssh to connect and run the command before returning
                if (nowait):
                    time.sleep(1)
                if result == 0:
                    return 0
                else:
                    return 1
            else:
                # send command and read stdout
                rtn_cmd = subprocess.Popen(sshCommand, shell=True, stdout=subprocess.PIPE)
                rtn_stdout = rtn_cmd.communicate()[0].strip()
                # If nowait, sleep for a second to give time for the ssh to connect and run the command before returning
                if (nowait):
                    time.sleep(1)
                return rtn_stdout 
        finally:
            if (ClientControl.logfile != None):
                self.restoreOutput()

    def isOnline(self):
        result = self.runCommand("wget -O /dev/null -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        return result

    # FIXME this should be in a test util class
    def check_events(self, events, num_events, *args, **kwargs):
        if events == None:
            return False
        if num_events == 0:
            return False
        if kwargs.get('min_date') == None:
            min_date = datetime.datetime.now()-datetime.timedelta(minutes=10)
        else:
            min_date = kwargs.get('min_date')
        if (len(args) % 2) != 0:
            print "Invalid argument length"
            return False
        num_checked = 0
        while num_checked < num_events:
            if len(events) <= num_checked:
                break
            event = events[num_checked]
            num_checked += 1

            # if event has a date and its too old - ignore the event
            if event.get('time_stamp') != None and datetime.datetime.fromtimestamp((event['time_stamp']['time'])/1000) < min_date:
                continue

            # check each expected value
            # if one doesn't match continue to the next event
            # if all match, return True
            allMatched = True
            for i in range(0, len(args)/2):
                key=args[i*2]
                expectedValue=args[i*2+1]
                actualValue = event.get(key)
                #print "key %s expectedValue %s actualValue %s " % ( key, str(expectedValue), str(actualValue) )
                if str(expectedValue) != str(actualValue):
                    print "mismatch event[%s] expectedValue %s != actualValue %s " % ( key, str(expectedValue), str(actualValue) )
                    allMatched = False
                    break

            if allMatched:
                return True
        return False
