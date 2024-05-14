# -*-ruby-*-

class Jars
  ## Makes the target with the downloads path prepended
  def Jars.downloadTarget(path)
    p =  "#{BuildEnv::downloads}/#{path}"
    if File.exist?(p)
      ThirdpartyJar.get(p)
    else
      b = File.basename(path)
      paths = [ "/usr/share/java/uvm/#{b}" ]
      p = paths.find do |f|
        File.exist?(f)
      end
      
      if p.nil?
        h = ([ "#{BuildEnv::downloads}/#{path}" ] + paths).map do |p|
          "#{p}: #{File.exist?(p)}"
        end

        warn "Could not find #{path} (#{h.join(',')})"
        warn "Could not find #{p}"
      else
        ThirdpartyJar.get(p)
      end
    end
  end
  
  def Jars.makeGroup(*jars)
    [ jars ].flatten.compact.uniq
  end

  def Jars.findJars
    ## Named groups of jars
    const_set(:CommonsLang, [ Jars.downloadTarget('commons-lang3-3.9/commons-lang3-3.9.jar') ])
    const_set(:Log4j, [ Jars.downloadTarget('apache-log4j-1.2.16/log4j-1.2.16.jar') ])
    const_set(:JavaMailApi, [ Jars.downloadTarget('javamail-1.3.3_01/lib/mailapi.jar') ])
    const_set(:Jabsorb, [ Jars.downloadTarget('jabsorb-1.2.2/jabsorb-1.2.2.jar')])
    const_set(:Json, [ Jars.downloadTarget('jabsorb-1.2.2/json.jar')])
    const_set(:GetText, [ Jars.downloadTarget('gettext-commons-0.9.1/gettext-commons-0.9.1.jar') ])
    const_set(:JakartaActivation, [ Jars.downloadTarget('jakarta.activation-1.2.1/jakarta.activation-1.2.1.jar') ])
    const_set(:JavaTransaction, [ Jars.downloadTarget('jta-1.1/jta-1.1.jar') ])
    const_set(:Slf4j, [ Jars.downloadTarget( 'slf4j-1.4.3/slf4j-log4j12-1.4.3.jar'), 
                        Jars.downloadTarget( 'slf4j-1.4.3/slf4j-api-1.4.3.jar' ) ])

    const_set(:TomcatCommon, [ 'tomcat-embed-jasper.jar',
                               'tomcat-embed-core.jar',
                               'tomcat-embed-el.jar',
                            ].map do |n|
                Jars.downloadTarget("apache-tomcat-8.5.83-embed/#{n}")
              end)

    const_set(:TomcatServer, ['ecj-4.6.3.jar',
                            ].map do |n|
                Jars.downloadTarget("apache-tomcat-8.5.83-embed/#{n}")
              end)
    
    const_set(:TomcatEmb, TomcatCommon + TomcatServer + [Jars.downloadTarget('httpcomponents-client-4.5.1/lib/commons-logging-1.2.jar')])
    
    ## Miscellaneous Jars
    const_set(:JavaMail, [ Jars.downloadTarget('javamail-1.3.3_01/mail.jar') ])
    const_set(:Postgres, [ Jars.downloadTarget('postgresql-9.4-1201.jdbc4/postgresql-9.4-1201.jdbc4.jar')])
    const_set(:Velocity, [ Jars.downloadTarget('velocity-1.4/velocity-1.4.jar'),
                           Jars.downloadTarget('velocity-1.4/velocity-dep-1.4.jar')])

    const_set(:JRadius, [Jars.downloadTarget('jradius-client-1.0.0-release/jradius/lib/commons-pool-1.6.jar'),
                         Jars.downloadTarget('jradius-client-1.0.0-release/jradius/lib/gnu-crypto-2.0.1.jar'),
                         Jars.downloadTarget('jradius-client-1.0.0-release/jradius/lib/jradius-core-1.0.0.jar'),
                         Jars.downloadTarget('jradius-client-1.0.0-release/jradius/lib/jradius-dictionary-1.0.0.jar')])

    const_set(:DnsJava, [ Jars.downloadTarget('dnsjava-3.5.0-20211020/dnsjava-3.5.0-20211020.jar') ])

    const_set(:SqlLite, [ Jars.downloadTarget('sqlite-jdbc-3.15.1/sqlite-jdbc-3.15.1.jar') ])
    
    const_set(:HttpClient, [ Jars.downloadTarget('httpcomponents-client-4.5.1/lib/httpclient-4.5.1.jar'),
                             Jars.downloadTarget('httpcomponents-client-4.5.1/lib/httpcore-4.4.3.jar'),
                             Jars.downloadTarget('httpcomponents-client-4.5.1/lib/commons-codec-1.9.jar'),
                             Jars.downloadTarget('commons-io-1.1/commons-io-1.1.jar'),
                             Jars.downloadTarget('commons-fileupload-1.1/commons-fileupload-1.1.jar')])

    const_set(:Selenium, [ Jars.downloadTarget('selenium-java-3.141.59/client-combined-3.141.59.jar'),
                           Jars.downloadTarget('selenium-java-3.141.59/libs/byte-buddy-1.8.15.jar'),
                           Jars.downloadTarget('selenium-java-3.141.59/libs/guava-25.0-jre.jar'),
                           Jars.downloadTarget('selenium-java-3.141.59/libs/commons-exec-1.3.jar'),
                           Jars.downloadTarget('selenium-java-3.141.59/libs/okhttp-3.11.0.jar'),
                           Jars.downloadTarget('selenium-java-3.141.59/libs/okio-1.14.0.jar')])
    
    const_set(:Jstl, [ Jars.downloadTarget('apache-taglibs-standard-1.2.5/taglibs-standard-impl-1.2.5.jar'),
                       Jars.downloadTarget('apache-taglibs-standard-1.2.5/taglibs-standard-spec-1.2.5.jar'),
                       Jars.downloadTarget('apache-taglibs-standard-1.2.5/taglibs-standard-jstlel-1.2.5.jar'),
                       Jars.downloadTarget('apache-taglibs-standard-1.2.5/taglibs-standard-compat-1.2.5.jar') ])

    const_set(:GeoIP, [ Jars.downloadTarget('geoip2-2.17.0-with-dependencies/geoip2-2.17.0/lib/jackson-annotations-2.16.0.jar'),
                           Jars.downloadTarget('geoip2-2.17.0-with-dependencies/geoip2-2.17.0/lib/jackson-databind-2.16.0.jar'),
                           Jars.downloadTarget('geoip2-2.17.0-with-dependencies/geoip2-2.17.0/lib/jackson-core-2.16.0.jar'),
                           Jars.downloadTarget('geoip2-2.17.0-with-dependencies/geoip2-2.17.0/lib/maxmind-db-2.1.0.jar'),
                           Jars.downloadTarget('geoip2-2.17.0-with-dependencies/geoip2-2.17.0/lib/geoip2-2.17.0.jar')])

    ## Groups used for compiling
    # This is available to everything?
    const_set(:Base, Jars.makeGroup(CommonsLang, Log4j, Postgres, JavaMailApi,
                                   GetText, JavaMail, TomcatEmb, Velocity, 
                                   HttpClient, Jstl, Json, Jabsorb,
                                   Slf4j, DnsJava, Selenium, GeoIP, SqlLite,
                                   JakartaActivation, JavaTransaction))

    const_set(:JDKTools, [ ThirdpartyJar.get("#{ENV['JAVA_HOME']}/lib/tools.jar") ])
  end
end
