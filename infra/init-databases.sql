-- Crea las bases de datos de VetSync (idempotente).
-- Lo ejecuta el servicio db-init de docker-compose.yml; el esquema lo crea Flyway al arrancar cada servicio.
IF DB_ID(N'vetsync_clinic') IS NULL CREATE DATABASE vetsync_clinic;
IF DB_ID(N'vetsync_citas') IS NULL CREATE DATABASE vetsync_citas;
IF DB_ID(N'vetsync_notificaciones') IS NULL CREATE DATABASE vetsync_notificaciones;
