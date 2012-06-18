
-- Go for MyISAM, lots of data foreseen.

create table wifi
(
  -- The id is a SHA1 (so max 40 chars)
  id varchar(40),
  -- The bssid is a MAC address (so max 18 including ':')
  bssid varchar(18),
  -- Undefinite length for ssid
  ssid varchar(255),
  -- Being a list also here undefined length
  capabilities varchar(255),
  --
  security integer,
  --
  `level` integer,
  --
  frequency integer,
  --
  lat double,
  --
  lon double,
  --
  alt double,
  --
  geohash varchar(255),
  --
  `timestamp` datetime,
  --
  primary key(id),
  key wifi_search (security, lat, lon)
)
engine=MyISAM default charset=utf8;

-- Users table
create table users
(
  username varchar(255),
  password varchar(255),
  primary key(username)
)
engine=MyISAM default charset=utf8;