
BASEDIR=$1
VERSION=$2
PREVIOUS_VERSION=$3

echo $BASEDIR
rm -rf "$BASEDIR/src"
cp "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.0-$VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.0-$PREVIOUS_VERSION.zip"
cp "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-$VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-$PREVIOUS_VERSION.zip"
cp "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-different-conf-$VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-different-conf-$PREVIOUS_VERSION.zip"
cp "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-different-conf-$VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-different-conf-$PREVIOUS_VERSION.zip"