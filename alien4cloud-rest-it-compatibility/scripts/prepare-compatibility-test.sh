
BASEDIR=$1
PREVIOUS_VERSION=$2

echo $BASEDIR
rm -rf "$BASEDIR/src"
cp "$BASEDIR/plugins-mock/$PREVIOUS_VERSION/alien4cloud-mock-paas-provider-plugin-1.0-$PREVIOUS_VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.0-$PREVIOUS_VERSION.zip"
cp "$BASEDIR/plugins-mock/$PREVIOUS_VERSION/alien4cloud-mock-paas-provider-plugin-1.1-$PREVIOUS_VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-$PREVIOUS_VERSION.zip"
cp "$BASEDIR/plugins-mock/$PREVIOUS_VERSION/alien4cloud-mock-paas-provider-plugin-$PREVIOUS_VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-$PREVIOUS_VERSION.zip"
cp "$BASEDIR/plugins-mock/$PREVIOUS_VERSION/alien4cloud-mock-paas-provider-plugin-1.1-different-conf-$PREVIOUS_VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.1-different-conf-$PREVIOUS_VERSION.zip"
cp "$BASEDIR/plugins-mock/$PREVIOUS_VERSION/alien4cloud-mock-paas-provider-plugin-invalid-$PREVIOUS_VERSION.zip" "$BASEDIR/../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-invalid-$PREVIOUS_VERSION.zip"
