import java.util.concurrent.TimeUnit
import org.apache.commons.validator.routines.InetAddressValidator;

def ipValidator = InetAddressValidator.getInstance();

assert MY_HOSTNAME : "Empty env var MY_HOSTNAME"
println "MY_HOSTNAME : ${MY_HOSTNAME}"
assert TARGET_HOSTNAME : "Empty env var TARGET_HOSTNAME"
println "TARGET_HOSTNAME : ${TARGET_HOSTNAME}"
assert MY_IP && ipValidator.isValidInet4Address(MY_IP): "Empty or not valid env var MY_IP"
println "MY_IP : ${MY_IP}"
assert TARGET_IP && ipValidator.isValidInet4Address(TARGET_IP): "Empty or not valid env var TARGET_IP"
println "TARGET_IP : ${TARGET_IP}"
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

sourcesArray.each{
  def name = it+"_MY_IP"
  assert binding.getVariable(name) && ipValidator.isValidInet4Address(binding.getVariable(name)): "Empty or malformed env var ${name}"
  println "${name} : ${binding.getVariable(name)}"
}

def targetsArray = TARGETS.split(",")
def nbTarget = targetsArray.length;
println "Nb of targets is ${nbTarget}: ${targetsArray}"

return SOURCE +"(${MY_IP})...."+TARGET+"(${TARGET_IP})"
