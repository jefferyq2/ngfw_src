/**
 * $Id: ApplicationControlProtoList.java 40861 2015-08-03 23:41:20Z mahotz $
 */
package com.untangle.app.application_control;

import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Comparator;
import java.text.Collator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class ApplicationControlProtoList
{
    private final Logger logger = Logger.getLogger(getClass());
    private ApplicationControlApp app;

    public ApplicationControlProtoList(ApplicationControlApp app)
    {
        this.app = app;
    }

    /*
     * The following insanity was created to allow changes and additions to the
     * Vineyard protocol list to be incorporated in the list of protocols that
     * are saved in the app settings. First we look for protocols that have been
     * removed from vineyard and remove them from our working list. Then we look
     * for new protocols added to Vineyard that we don't know about and add them
     * to our working list. In the middle of all of this we look for differences
     * between our rules and the vineyard rules so we can pull in any chages to
     * name, description, category, etc. We use a counter to keep track of
     * differences so we can return null if nothing changed.
     */

    public LinkedList<ApplicationControlProtoRule> mergeProtoList(LinkedList<ApplicationControlProtoRule> loadedList)
    {
        LinkedList<ApplicationControlProtoRule> vineyardList = buildProtoList();
        LinkedList<ApplicationControlProtoRule> masterList = new LinkedList<ApplicationControlProtoRule>();
        Hashtable<String, ApplicationControlProtoRule> vineyardHash = new Hashtable<String, ApplicationControlProtoRule>();
        Hashtable<String, ApplicationControlProtoRule> masterHash = new Hashtable<String, ApplicationControlProtoRule>();
        ApplicationControlProtoRule local, find;
        int changeCount = 0;
        int x;

        // put all the vineyard rules into a hashtable
        for (x = 0; x < vineyardList.size(); x++) {
            local = vineyardList.get(x);
            vineyardHash.put(local.getGuid(), local);
        }

        // check to make sure all of the loaded rules still exist
        for (x = 0; x < loadedList.size(); x++) {
            local = loadedList.get(x);
            find = vineyardHash.get(local.getGuid());

            // good rule found so add to the master list
            if (find != null) {
                // if nothing has changed we keep the one we have
                if (find.isTheSameAs(local)) {
                    masterList.add(local);
                }

                // if something has changed we update all of our fields
                else {
                    local.setName(find.getName());
                    local.setDescription(find.getDescription());
                    local.setCategory(find.getCategory());
                    local.setProductivity(find.getProductivity());
                    local.setRisk(find.getRisk());
                    masterList.add(local);
                    logger.info("PROTOLIST updating modified rule " + local.getGuid());
                    changeCount += 1;
                }
            }

            else {
                logger.info("PROTOLIST removing stale rule " + local.getGuid());
                changeCount += 1;
            }
        }

        // now put everything that made it into the master list
        // into the master hash so we can look for missing protocols
        for (x = 0; x < masterList.size(); x++) {
            local = masterList.get(x);
            masterHash.put(local.getGuid(), local);
        }

        // check every entry in the vineyard list to see if it exists in the
        // master hash which was created with a cleaned up version of the
        // loaded list. Confused yet?
        for (x = 0; x < vineyardList.size(); x++) {
            local = vineyardList.get(x);
            find = masterHash.get(local.getGuid());

            // found new vineyard protocol so we add to our working list
            if (find == null) {
                masterList.add(local);
                logger.info("PROTOLIST inserting new rule " + local.getGuid());
                changeCount += 1;
            }
        }

        // if nothing changed just return null
        if (changeCount == 0) return (null);

        // sort the new and improved protocol list by name
        masterList.sort(new Comparator<ApplicationControlProtoRule>()
        {
            @Override
            public int compare(ApplicationControlProtoRule r1, ApplicationControlProtoRule r2)
            {
                return Collator.getInstance().compare(r1.getName(), r2.getName());
            }
        });

        // return the new and improved protocol list
        return (masterList);
    }

    /*
     * The vineyard protocol list is created by loading and parsing the metadata
     * file that is provided by Vineyard in CSV format. I'm using a very simple
     * state machine thingy to parse the ten fields. It handles both quoted and
     * unquoted fields, commas within a quoted field, and even escaped quotes
     * within a quoted field. Other than that, all bets are off.
     */

    public LinkedList<ApplicationControlProtoRule> buildProtoList()
    {
        LinkedList<ApplicationControlProtoRule> ruleList = new LinkedList<ApplicationControlProtoRule>();
        ApplicationControlProtoRule protoRule = null;

        try {
            File protofile = new File("/usr/share/untangle-classd/protolist.csv");
            BufferedReader reader = new BufferedReader(new FileReader(protofile));
            StringBuilder[] field = new StringBuilder[10];
            String grabstr;
            boolean eflag, qflag;
            int linetot = 0;
            int index, len, x;
            char bite;

            for (;;) {
                // grab a line from the file
                grabstr = reader.readLine();
                if (grabstr == null) break;
                len = grabstr.length();

                // increment the line counter
                linetot++;

                // first line is the field description header
                if (linetot == 1) continue;

                // new string buffer for each of the fields
                for (x = 0; x < 10; x++)
                    field[x] = new StringBuilder(1024);

                // clear the quote and escape flags and zero the field index
                qflag = eflag = false;
                index = 0;

                for (x = 0; x < len; x++) {
                    bite = grabstr.charAt(x);

                    // if we find a comma while not in quoted field mode
                    // then we increment the field index and continue
                    if ((bite == ',') && (qflag == false)) {
                        index++;
                        continue;
                    }

                    // if we find a quote toggle the qflag and continue
                    if (bite == '"') {
                        qflag = (qflag ? false : true);
                        continue;
                    }

                    // handle escaped quote characters within a quoted field
                    // by bumping the index and setting bite to the next
                    // char (quote) which will be appened to the current field
                    if ((qflag == true) && (bite == '\\') && (grabstr.charAt(x + 1) == '"')) {
                        x++;
                        bite = grabstr.charAt(x + 1);
                    }

                    field[index].append(bite);
                }

                // create a new rule and add to the list
                protoRule = new ApplicationControlProtoRule(field[0].toString(), // guid
                        false, // block
                        false, // tarpit
                        false, // flag
                        field[2].toString(), // name
                        field[3].toString(), // description
                        field[4].toString(), // category
                        Integer.parseInt(field[5].toString()), // productivity
                        Integer.parseInt(field[6].toString())); // risk

                ruleList.add(protoRule);
            }
        }

        catch (Exception e) {
            logger.error("buildProtoList()", e);
        }

        // set flagging on any of the proxy categories
        // set flagging on any of the games categories
        // set flagging on any of the p2p apps
        for (ApplicationControlProtoRule rule : ruleList) {
            String cat = rule.getCategory();
            String app = rule.getGuid();

            if ("Proxy".equals(cat)) {
                rule.setFlag(true);
            }
            if ("Games".equals(cat)) {
                rule.setFlag(true);
            }
            if ("BITTORRE".equals(app) || "EDONKEY".equals(app) || "GNUTELLA".equals(app) || "KAZAA".equals(app) || "IMESH".equals(app) || "WINMX".equals(app)) {
                rule.setFlag(true);
            }
        }

        return (ruleList);
    }
}
