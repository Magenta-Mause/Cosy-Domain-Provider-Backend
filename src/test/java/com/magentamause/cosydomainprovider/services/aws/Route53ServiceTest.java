package com.magentamause.cosydomainprovider.services.aws;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.aws.Route53Properties;
import com.magentamause.cosydomainprovider.model.dns.DnsEntry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

@ExtendWith(MockitoExtension.class)
class Route53ServiceTest {

    @Mock private Route53Client route53Client;

    private Route53Properties props;
    private Route53Service service;

    @BeforeEach
    void setUp() {
        props = new Route53Properties();
        props.setHostedZoneId("ZONE123");
        props.setDomain("cosy-hosting.net");
        props.setDefaultTtl(60L);
        service = new Route53Service(route53Client, props);
    }

    private ChangeResourceRecordSetsResponse changeResponse(String id) {
        return ChangeResourceRecordSetsResponse.builder()
                .changeInfo(ChangeInfo.builder().id(id).build())
                .build();
    }

    @Test
    void upsertARecord_submitsChangeAndReturnsId() {
        when(route53Client.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class)))
                .thenReturn(changeResponse("/change/UPSERT-A"));

        String id = service.upsertARecord("sub.cosy-hosting.net", "1.2.3.4");

        assertThat(id).isEqualTo("/change/UPSERT-A");
        verify(route53Client).changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class));
    }

    @Test
    void deleteARecord_submitsChangeAndReturnsId() {
        when(route53Client.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class)))
                .thenReturn(changeResponse("/change/DELETE-A"));

        String id = service.deleteARecord("sub.cosy-hosting.net", "1.2.3.4");

        assertThat(id).isEqualTo("/change/DELETE-A");
    }

    @Test
    void upsertAAAARecord_submitsChangeAndReturnsId() {
        when(route53Client.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class)))
                .thenReturn(changeResponse("/change/UPSERT-AAAA"));

        String id = service.upsertAAAARecord("sub.cosy-hosting.net", "::1");

        assertThat(id).isEqualTo("/change/UPSERT-AAAA");
    }

    @Test
    void deleteAAAARecord_submitsChangeAndReturnsId() {
        when(route53Client.changeResourceRecordSets(any(ChangeResourceRecordSetsRequest.class)))
                .thenReturn(changeResponse("/change/DELETE-AAAA"));

        String id = service.deleteAAAARecord("sub.cosy-hosting.net", "::1");

        assertThat(id).isEqualTo("/change/DELETE-AAAA");
    }

    @Test
    void listAllRecords_singlePage_returnsAllEntries() {
        ResourceRecordSet rrs =
                ResourceRecordSet.builder()
                        .name("sub.cosy-hosting.net.")
                        .type(RRType.A)
                        .ttl(60L)
                        .resourceRecords(ResourceRecord.builder().value("1.2.3.4").build())
                        .build();

        ListResourceRecordSetsResponse response =
                ListResourceRecordSetsResponse.builder()
                        .resourceRecordSets(rrs)
                        .isTruncated(false)
                        .build();

        when(route53Client.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
                .thenReturn(response);

        List<DnsEntry> entries = service.listAllRecords();

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).name()).isEqualTo("sub.cosy-hosting.net.");
        assertThat(entries.get(0).type()).isEqualTo("A");
        assertThat(entries.get(0).values()).containsExactly("1.2.3.4");
    }

    @Test
    void listAllRecords_paginated_fetchesAllPages() {
        ResourceRecordSet rrs1 =
                ResourceRecordSet.builder()
                        .name("a.cosy-hosting.net.")
                        .type(RRType.A)
                        .ttl(60L)
                        .resourceRecords(ResourceRecord.builder().value("1.1.1.1").build())
                        .build();

        ResourceRecordSet rrs2 =
                ResourceRecordSet.builder()
                        .name("b.cosy-hosting.net.")
                        .type(RRType.A)
                        .ttl(60L)
                        .resourceRecords(ResourceRecord.builder().value("2.2.2.2").build())
                        .build();

        ListResourceRecordSetsResponse page1 =
                ListResourceRecordSetsResponse.builder()
                        .resourceRecordSets(rrs1)
                        .isTruncated(true)
                        .nextRecordName("b.cosy-hosting.net.")
                        .nextRecordType("A")
                        .build();

        ListResourceRecordSetsResponse page2 =
                ListResourceRecordSetsResponse.builder()
                        .resourceRecordSets(rrs2)
                        .isTruncated(false)
                        .build();

        when(route53Client.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
                .thenReturn(page1, page2);

        List<DnsEntry> entries = service.listAllRecords();

        assertThat(entries).hasSize(2);
        verify(route53Client, times(2))
                .listResourceRecordSets(any(ListResourceRecordSetsRequest.class));
    }

    @Test
    void listARecords_filtersToARecordsOnly() {
        ResourceRecordSet aRecord =
                ResourceRecordSet.builder()
                        .name("sub.cosy-hosting.net.")
                        .type(RRType.A)
                        .ttl(60L)
                        .resourceRecords(ResourceRecord.builder().value("1.2.3.4").build())
                        .build();

        ResourceRecordSet nsRecord =
                ResourceRecordSet.builder()
                        .name("cosy-hosting.net.")
                        .type(RRType.NS)
                        .ttl(300L)
                        .resourceRecords(ResourceRecord.builder().value("ns1.example.com").build())
                        .build();

        ListResourceRecordSetsResponse response =
                ListResourceRecordSetsResponse.builder()
                        .resourceRecordSets(aRecord, nsRecord)
                        .isTruncated(false)
                        .build();

        when(route53Client.listResourceRecordSets(any(ListResourceRecordSetsRequest.class)))
                .thenReturn(response);

        List<DnsEntry> aRecords = service.listARecords();

        assertThat(aRecords).hasSize(1);
        assertThat(aRecords.get(0).type()).isEqualTo("A");
    }
}
