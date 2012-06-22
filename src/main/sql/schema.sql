
-- Go for MyISAM, lots of data foreseen.

create table wifi
(
  -- The id is a SHA1 (so max 40 chars) + the username
  id varchar(40),
  fk_user varchar(255),
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
  t_timestamp datetime,
  --
  -- for possible future use, and also for correctness, create the constraint anyway
  constraint foreign key fk_user(fk_user) references users(username) on delete cascade,
  --
  primary key(id, fk_user),
  key wifi_search (fk_user, security, lat, lon),
  key wifi_sync (fk_user, t_timestamp)
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