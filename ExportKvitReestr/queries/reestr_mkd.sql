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
                  from lcmccb.CM_KVEE_MKD_CSV t
                 where pdat = &pdat
                   and (&blank_unk = '-1' 
                        or
                        &blank_unk = '0' and trim(t.ls) is null
                        or
                        &blank_unk = '1' and trim(t.ls) is not null)
                   and ((&use_filter != '1'
                       and &mkd_id = '-1'
                       and leskgesk = &pleskgesk
                       and bd_lesk = &bd_lesk
                        or &use_filter = '1')
                    or (nvl(&mkd_id, '-1') = '-1'
                        and &use_filter != '1'
                        and leskgesk = &pleskgesk
                        and bd_lesk = &bd_lesk
                         or &mkd_id != '-1'
                        and t.bill_id in (select bs.bill_id
                                            from rusadm.ci_bseg bs
                                           where trunc(bs.end_dt, 'mm') = &pdat
                                             and bs.bseg_stat_flg = 50
                                             and exists (select null
                                                           from rusadm.ci_prem  pr
                                                          where pr.prem_id = bs.prem_id
                                                            and pr.prnt_prem_id = &mkd_id))))
                 order by t.bd_lesk,
                          upper(t.addressshort),
                          upper(t.address3),
                          to_number(regexp_replace(t.address2,'[^[[:digit:]]]*')),
                          upper(t.address2),
                          to_number(regexp_replace(t.address4,'[^[[:digit:]]]*')),
                          upper(t.address4)) a)
group by address,
         bd_lesk
order by min(rn);
