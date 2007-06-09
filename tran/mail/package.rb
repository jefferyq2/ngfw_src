# -*-ruby-*-

NodeBuilder.makeCasing(BuildEnv::SRC, 'mail')

mail = BuildEnv::SRC['mail-casing']

jt = [mail['localapi']]

ServletBuilder.new(mail, 'com.untangle.node.mail.quarantine.jsp',
                   "#{SRC_HOME}/tran/mail/servlets/quarantine", [], jt)

