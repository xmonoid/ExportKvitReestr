select extractValue(XMLType(lv.BO_DATA_AREA), 'tarrifTable/company') company
  from rusadm.F1_EXT_LOOKUP_VAL lv
 where trim(lv.bus_obj_cd) = case
                               when to_number(&bd_lesk) between 1 and 18 then
                                 'CM_EL_ORG'
                               else
                                 'CM_EL_ORG_G'
                             end
   and nvl(extractValue(XMLType(lv.BO_DATA_AREA), 'tarrifTable/kod_bd'), &bd_lesk)  = &bd_lesk
   and rownum = 1;

select address      "ADDRESS",
       min(rn)      "MIN",
       max(rn)      "MAX",
       count(rn)    "COUNT",
       bd_lesk      "BD_LESK"
  from (select rownum    rn,
               address,
               ls,
               bd_lesk
          from (select t.addressshort address,
                       t.ls,
                       t.barcode,
                       t.bd_lesk
                  from lcmccb.cm_kvee_notmkd_csv t
                 where pdat = &pdat
                   and leskgesk = &pleskgesk
                    and bd_lesk = case &use_filter
                                   when 'true' then
                                     bd_lesk
                                   else
                                     &bd_lesk
                                 end

                         order by t.bd_lesk,
                          upper(t.addressshort),
                          upper(t.address3),
                          to_number(regexp_replace(t.address2,'[^[[:digit:]]]*')),
                          upper(t.address2),
                          to_number(regexp_replace(t.address4,'[^[[:digit:]]]*')),
                          upper(t.address4)) a)
group by address,
         bd_lesk
order by bd_lesk,
         min(rn);