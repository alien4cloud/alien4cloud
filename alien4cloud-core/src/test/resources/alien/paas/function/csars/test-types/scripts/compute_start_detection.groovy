def name = "hostname".execute().text.trim()
println "Got HostName: ${name}; Expected one is ${EXPECTED_HOSTNAME}"
return EXPECTED_HOSTNAME == name