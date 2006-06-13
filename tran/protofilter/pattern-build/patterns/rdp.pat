# RDP - Remote Desktop Protocol (used in Windows Terminal Services)
# Pattern attributes: ok fast
# Protocol groups: proprietary remote_access
# Wiki: http://www.protocolinfo.org/wiki/RDP
#
# This pattern was submitted by Michael Leong.  It has been tested under the 
# following conditions: "WinXP Pro with all the patches, rdesktop server 
# running on port 7000 instead of 3389 --> WinXP Pro Remote Desktop Client."
# Also tested is WinXP to Win 2000 Server.

rdp
rdpdr.*cliprdr.*rdpsnd

# Old pattern, submitted by Daniel Weatherford.
# rdpdr.*cliprdp.*rdpsnd 


