import java.util.concurrent.TimeUnit
import org.apache.commons.validator.routines.InetAddressValidator;

def ipValidator = InetAddressValidator.getInstance();

assert MY_HOSTNAME : "Empty env var MY_HOSTNAME"
println "MY_HOSTNAME : ${MY_HOSTNAME}"
assert SOURCE_HOSTNAME : "Empty env var SOURCE_HOSTNAME"
println "SOURCE_HOSTNAME : ${SOURCE_HOSTNAME}"
assert MY_IP && ipValidator.isValidInet4Address(MY_IP): "Empty or not valid env var MY_IP"
println "MY_IP : ${MY_IP}"
assert SOURCE_IP && ipValidator.isValidInet4Address(SOURCE_IP): "Empty or not valid env var SOURCE_IP"
println "SOURCE_IP : ${SOURCE_IP}"
assert SOURCE : "Empty env var SOURCE"
println "SOURCE : ${SOURCE}"
assert SOURCE_NAME : "Empty env var SOURCE_NAME"
println "SOURCE_NAME : ${SOURCE_NAME}"
assert SOURCE_SERVICE_NAME : "Empty env var SOURCE_SERVICE_NAME"
println "SOURCE_SERVICE_NAME : ${SOURCE_SERVICE_NAME}"
assert SOURCES : "Empty env var SOURCES"
println "SOURCES : ${SOURCES}"
assert TARGET : "Empty env var TARGET"
println "TARGET : ${TARGET}"
assert TARGET_NAME : "Empty env var TARGET_NAME"
println "TARGET_NAME : ${TARGET_NAME}"
assert TARGET_SERVICE_NAME : "Empty env var TARGET_SERVICE_NAME"
println "TARGET_SERVICE_NAME : ${TARGET_SERVICE_NAME}"
assert TARGETS : "Empty env var TARGETS"
println "TARGETS : ${TARGETS}"

def sourcesArray = SOURCES.split(",")
def nbSource = sourcesArray.length;
println "Nb of sources is ${nbSource}: ${sourcesArray}"

def targetsArray = TARGETS.split(",")
def nbTarget = targetsArray.length;
println "Nb of targets is ${nbTarget}: ${targetsArray}"

targetsArray.each{
  def name = it+"_MY_IP"
  assert binding.getVariable(name) : "Empty env var ${name}"
  println "${name} : ${binding.getVariable(name)}"
}

return TARGET+"...."+SOURCE;
