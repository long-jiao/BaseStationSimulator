DROP table if exists `PERIODIC_Parameter`;
create table PERIODIC_Parameter
(
    `id`           integer NOT NULL primary key auto_increment,
    `paraPath` VARCHAR(100)
);

INSERT INTO PERIODIC_Parameter (paraPath) values ('Device.RootDataModelVersion');
INSERT INTO PERIODIC_Parameter (paraPath) values ('Device.DeviceInfo.MU.1.HardwareVersion');
INSERT INTO PERIODIC_Parameter (paraPath) values ('Device.DeviceInfo.MU.1.SoftwareVersion');
INSERT INTO PERIODIC_Parameter (paraPath) values ('Device.DeviceInfo.MU.1.ProvisioningCode');
INSERT INTO PERIODIC_Parameter (paraPath) values ('Device.ManagementServer.ParameterKey');
INSERT INTO PERIODIC_Parameter (paraPath) values ('Device.ManagementServer.ConnectionRequestURL');
