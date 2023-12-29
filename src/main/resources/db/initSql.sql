DROP table if exists `DeviceInfo`;
create table DeviceInfo
(
    `id`           integer NOT NULL primary key auto_increment,
    `manufacturer` VARCHAR(20),
    `oui`          VARCHAR(10),
    `productClass` VARCHAR(10)
);

INSERT INTO DeviceInfo (id, manufacturer, oui, productClass) values (1, 'CHANGKUN', '7C8334', 'L5W6-BS');