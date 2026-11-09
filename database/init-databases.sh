#!/bin/sh
set -eu

: "${MYSQL_HOST:=mysql}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"
: "${MYSQL_APP_USER:=lab_app}"
: "${MYSQL_APP_PASSWORD:?MYSQL_APP_PASSWORD is required}"

mysql -h "$MYSQL_HOST" -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS lab_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_resource DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_booking DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Kept for the one-time approval data migration into lab_booking.
CREATE DATABASE IF NOT EXISTS lab_approval DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '$MYSQL_APP_USER'@'%' IDENTIFIED BY '$MYSQL_APP_PASSWORD';
GRANT ALL PRIVILEGES ON lab_user.* TO '$MYSQL_APP_USER'@'%';
GRANT ALL PRIVILEGES ON lab_resource.* TO '$MYSQL_APP_USER'@'%';
GRANT ALL PRIVILEGES ON lab_booking.* TO '$MYSQL_APP_USER'@'%';
GRANT ALL PRIVILEGES ON lab_approval.* TO '$MYSQL_APP_USER'@'%';
GRANT ALL PRIVILEGES ON lab_system.* TO '$MYSQL_APP_USER'@'%';
FLUSH PRIVILEGES;
SQL

echo "Databases and application user are ready."
