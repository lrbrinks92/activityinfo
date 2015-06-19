
-- Creates a "nullary" location or location at the "country" level to fake
-- forms with "no" geography until the new forms model is complete

insert into locationtype (name,reuse, countryid, workflowid)
  select "Country", 0, countryid, "closed" from country
    where countryid not in (select lt.countryid from locationtype lt where lt.name ='Country');

insert location (locationid, name, locationtypeid, x, y, timeEdited)
  select lt.locationtypeid, c.name, lt.locationtypeid, (c.x1+c.x2)/2, (c.y1+c.y2)/2,
    unix_timestamp(now())*1000 from locationtype lt left join country c on (lt.countryid=c.countryid)
      where lt.name='Country' and lt.locationtypeid not in (select locationid from location);