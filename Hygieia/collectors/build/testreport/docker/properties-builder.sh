#!/bin/bash

if [ "$SKIP_PROPERTIES_BUILDER" = true ]; then
  echo "Skipping properties builder"
  echo "`cat $PROP_FILE`"
  exit 0
fi

cat > $PROP_FILE <<EOF
dbname=${MONGODB_DATABASE:-dashboarddb}
dbhost=${MONGODB_HOST:-db}
dbport=${MONGODB_PORT:-27017}
dbusername=${MONGODB_USERNAME:-dashboarduser}
dbpassword=${MONGODB_PASSWORD:-dbpassword}

testreport.cron=${SONAR_CRON:-0 */5 * * * *}

testreport.paths[0]=${TESTREPORT_URL:-/root/IPOFFICE-SIP-UNITTEST}

#testreport supportedformats
testreport.supportedformats[0]=${TESTREPORT_FORMATS}

#testreport Metrics
testreport.metrics[0]=${TESTREPORT_METRICS:-ncloc,line_coverage,violations,critical_violations,major_violations,blocker_violations,sqale_index,test_success_density,test_failures,test_errors,tests}


EOF

echo "

===========================================
Properties file created `date`:  $PROP_FILE
Note: passwords hidden
===========================================
`cat $PROP_FILE |egrep -vi password`
"

exit 0
