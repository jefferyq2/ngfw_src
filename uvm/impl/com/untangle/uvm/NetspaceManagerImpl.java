/**
 * $Id: NetspaceManagerImpl.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Iterator;
import org.apache.log4j.Logger;

import com.untangle.uvm.NetspaceManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.IPMaskedAddress;

/**
 * The NetspaceManager provides a centralized registry for network address
 * blocks that are in use by applications in the system.
 */
public class NetspaceManagerImpl implements NetspaceManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final LinkedList<NetworkSpace> networkRegistry = new LinkedList<NetworkSpace>();

    /**
     * Constructor
     */
    protected NetspaceManagerImpl()
    {
    }

    /**
     * Called to register a network address block in use by an application
     * 
     * @param ownerName
     *        The name of the owner
     * @param ownerPurpose
     *        What the network block is being used for
     * @param networkAddress
     *        The address of the network block
     * @param networkSize
     *        The size of the network block
     */
    public void registerNetworkBlock(String ownerName, String ownerPurpose, InetAddress networkAddress, Integer networkSize)
    {
        NetworkSpace space = new NetworkSpace();
        space.ownerName = ownerName;
        space.ownerPurpose = ownerPurpose;
        space.maskedAddress = new IPMaskedAddress(networkAddress, networkSize);
        networkRegistry.add(space);
        logger.debug("Added Netspace " + space.toString());
    }

    /**
     * Called to register a network address block in use by an application
     * 
     * @param ownerName
     *        The name of the owner
     * @param ownerPurpose
     *        What the network block is being used for
     * @param networkText
     *        The network
     */
    public void registerNetworkBlock(String ownerName, String ownerPurpose, String networkText)
    {
        NetworkSpace space = new NetworkSpace();
        space.ownerName = ownerName;
        space.ownerPurpose = ownerPurpose;
        space.maskedAddress = new IPMaskedAddress(networkText);
        networkRegistry.add(space);
        logger.debug("Added Netspace " + space.toString());
    }

    /**
     * Called to remove all registrations for an owner
     * 
     * @param ownerName
     *        The owner
     */
    public void clearOwnerRegistrationAll(String ownerName)
    {
        Iterator<NetworkSpace> nsi = networkRegistry.iterator();
        NetworkSpace space;

        while (nsi.hasNext()) {
            space = nsi.next();
            if (!ownerName.equals(space.ownerName)) continue;
            nsi.remove();
            logger.debug("Removed Netspace " + space.toString());
        }
    }

    /**
     * Called to remove all registrations for an owner
     * 
     * @param ownerName
     *        The owner name
     * @param ownerPurpose
     *        The owner purpose
     */
    public void clearOwnerRegistrationPurpose(String ownerName, String ownerPurpose)
    {
        Iterator<NetworkSpace> nsi = networkRegistry.iterator();
        NetworkSpace space;

        while (nsi.hasNext()) {
            space = nsi.next();
            if (!ownerName.equals(space.ownerName)) continue;
            if (!ownerPurpose.contentEquals(space.ownerPurpose)) continue;
            nsi.remove();
            logger.debug("Removed Netspace " + space.toString());
        }
    }

    /**
     * Called to determine if the passed network conflicts with any existing
     * network registrations
     * 
     * @param networkAddress
     *        The network address
     * @param networkSize
     *        The network size
     * @return true if the network block is available for use or false if it
     *         conflicts with an existing registration
     */
    public boolean isNetworkAvailable(InetAddress networkAddress, Integer networkSize)
    {
        IPMaskedAddress tester = new IPMaskedAddress(networkAddress, networkSize);
        return isNetworkAvailable(tester);
    }

    /**
     * Called to determine if the passed network conflicts with any existing
     * network registrations
     * 
     * @param networkText
     *        The network address
     * @return true if the network block is available for use or false if it
     *         conflicts with an existing registration
     */
    public boolean isNetworkAvailable(String networkText)
    {
        IPMaskedAddress tester = new IPMaskedAddress(networkText);
        return isNetworkAvailable(tester);
    }

    /**
     * Called to determine if the passed network conflicts with any existing
     * network registrations
     * 
     * @param tester
     *        The network to test
     * @return true if the network block is available for use or false if it
     *         conflicts with an existing registration
     */

    public boolean isNetworkAvailable(IPMaskedAddress tester)
    {
        Iterator<NetworkSpace> nsi = networkRegistry.iterator();
        NetworkSpace space;

        while (nsi.hasNext()) {
            space = nsi.next();
            if (tester.isIntersecting(space.maskedAddress)) return true;
        }

        return (true);
    }

    /**
     * getAvailableAddressSpace should be used to get an unregistered address space based on a random subnet
     * 
     * @return IPMaskedAddress - An IPv4 CIDR address that is not conflicting with other address spaces on the appliance
     */
    public IPMaskedAddress getAvailableAddressSpace() {

        // Gen a random address
        Random rand = new Random();
        IPMaskedAddress randAddress = null;
        boolean uniqueAddress = false;

        // If the address intersects another address, gen another one until we have one that is not matching
        do {
            randAddress = new IPMaskedAddress("172.16." + rand.nextInt(250) + ".0/24");

            for (IPMaskedAddress takenAddr : networkRegistry) {
                if(!takenAddr.isIntersecting(randAddress)) {
                    uniqueAddress = true;
                }
            }
        } while (!uniqueAddress);
        
        return randAddress;
    }
}
