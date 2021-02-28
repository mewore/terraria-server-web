#!/usr/bin/env bash

version=${1:-'0.0.1'}
jar_location="./terraria-server-web-${version}-SNAPSHOT.jar"
if ! [ -f "${jar_location}" ]; then
  echo "${jar_location} does not exist!"
  exit 1
fi
echo "${jar_location}"

TSW_DB_LOCATION_DEFAULT='localhost'
read -r -p "DB location [${TSW_DB_LOCATION_DEFAULT}]: " TSW_DB_LOCATION
if [ -z "${TSW_DB_LOCATION}" ]; then TSW_DB_LOCATION="${TSW_DB_LOCATION_DEFAULT}"; fi

TSW_DB_NAME_DEFAULT='terraria_server_web_prod'
read -r -p "DB name [${TSW_DB_NAME_DEFAULT}]: " TSW_DB_NAME
if [ -z "${TSW_DB_NAME}" ]; then TSW_DB_NAME="${TSW_DB_NAME_DEFAULT}"; fi

while [ -z "${TSW_DB_USERNAME}" ]; do
  read -r -p 'DB username: ' TSW_DB_USERNAME
done
while [ -z "${TSW_DB_PASSWORD}" ]; do
  read -r -sp 'DB password: ' TSW_DB_PASSWORD
  echo
done

export TSW_DB_LOCATION
export TSW_DB_NAME
export TSW_DB_USERNAME
export TSW_DB_PASSWORD
"${JAVA_HOME}"/bin/java -jar "${jar_location}" --spring.profiles.active=common,prod
