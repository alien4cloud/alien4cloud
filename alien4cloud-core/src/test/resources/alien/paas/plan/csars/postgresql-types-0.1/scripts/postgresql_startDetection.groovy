import org.cloudifysource.dsl.utils.ServiceUtils

def sleepTimeInMiliSeconds = 3300
while (ServiceUtils.arePortsFree([5432])) {
 println "sleeping for ${sleepTimeInMiliSeconds} mili seconds..."
 Thread.sleep(sleepTimeInMiliSeconds)
}