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
       bd_lesk      "BD_LESK",
       postal       "POSTAL"
  from (select rownum    rn,
               address,
               ls,
               bd_lesk,
               postal
          from (select t.addressshort address,
                       t.ls,
                       t.barcode,
                       t.bd_lesk,
                       (select pr.postal
                          from rusadm.ci_bill  b,
                               rusadm.ci_acct  ac,
                               rusadm.ci_prem  pr
                         where t.bill_id = b.bill_id
                           and b.acct_id = ac.acct_id
                           and ac.mailing_prem_id = pr.prem_id
                           and rownum = 1) postal
                  from lcmccb.CM_KVEE_MKD_CSV t
                 where pdat = &pdat
                   and leskgesk = &pleskgesk
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
                          postal,
                          upper(t.addressshort),
                          upper(t.address3),
                          to_number(regexp_replace(t.address2,'[^[[:digit:]]]*')),
                          upper(t.address2),
                          to_number(regexp_replace(t.address4,'[^[[:digit:]]]*')),
                          upper(t.address4)) a)
group by address,
         postal,
         bd_lesk
order by bd_lesk,
         postal,
         min(rn);