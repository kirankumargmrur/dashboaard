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

coverity.cron=${COVERITY_CRON:-0 */5 * * * *}

coverity.servers[0]=${COVERITY_URL:-http://localhost:9000}

coverity.username=$COVERITY_USERNAME
coverity.password=$COVERITY_PASSWORD

#Coverity Metrics
coverity.metrics[0]=${COVERITY_METRICS:-ncloc,line_coverage,violations,critical_violations,major_violations,blocker_violations,sqale_index,test_success_density,test_failures,test_errors,tests}


EOF

echo "

===========================================
Properties file created `date`:  $PROP_FILE
Note: passwords hidden
===========================================
`cat $PROP_FILE |egrep -vi password`
"

exit 0
