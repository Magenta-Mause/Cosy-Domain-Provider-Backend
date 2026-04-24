package com.magentamause.cosydomainprovider.services.aws;

import com.magentamause.cosydomainprovider.configuration.aws.Route53Properties;
import com.magentamause.cosydomainprovider.model.dns.DnsEntry;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

@Slf4j
@Service
public class Route53Service {

    private final Route53Client client;
    private final Route53Properties props;

    public Route53Service(Route53Client client, Route53Properties props) {
        this.client = client;
        this.props = props;
    }

    public String upsertARecord(String fqdn, String ip) {
        return submitChange(ChangeAction.UPSERT, fqdn, ip);
    }

    public String deleteARecord(String fqdn, String ip) {
        return submitChange(ChangeAction.DELETE, fqdn, ip);
    }

    /**
     * Lists all DNS records in the hosted zone. Handles pagination automatically to fetch all
     * records.
     */
    public List<DnsEntry> listAllRecords() {
        List<ResourceRecordSet> allRecords = new ArrayList<>();
        String nextRecordName = null;
        String nextRecordType = null;

        do {
            ListResourceRecordSetsRequest.Builder requestBuilder =
                    ListResourceRecordSetsRequest.builder().hostedZoneId(props.getHostedZoneId());

            if (nextRecordName != null) {
                requestBuilder.startRecordName(nextRecordName);
            }
            if (nextRecordType != null) {
                requestBuilder.startRecordType(nextRecordType);
            }

            ListResourceRecordSetsResponse response =
                    client.listResourceRecordSets(requestBuilder.build());

            allRecords.addAll(response.resourceRecordSets());

            if (Boolean.TRUE.equals(response.isTruncated())) {
                nextRecordName = response.nextRecordName();
                nextRecordType = response.nextRecordTypeAsString();
            } else {
                nextRecordName = null;
                nextRecordType = null;
            }
        } while (nextRecordName != null);

        log.info("Fetched {} records", allRecords.size());
        return allRecords.stream().map(this::toDnsEntry).toList();
    }

    /**
     * Lists only A records (filters out NS, SOA, and others). Useful if you only want user-created
     * subdomains.
     */
    public List<DnsEntry> listARecords() {
        return listAllRecords().stream().filter(r -> RRType.A.toString().equals(r.type())).toList();
    }

    private DnsEntry toDnsEntry(ResourceRecordSet r) {
        List<String> values = r.resourceRecords().stream().map(ResourceRecord::value).toList();
        return new DnsEntry(r.name(), r.typeAsString(), r.ttl(), values);
    }

    private String submitChange(ChangeAction action, String name, String ip) {
        ResourceRecordSet recordSet =
                ResourceRecordSet.builder()
                        .name(name)
                        .type(RRType.A)
                        .ttl(props.getDefaultTtl())
                        .resourceRecords(ResourceRecord.builder().value(ip).build())
                        .build();

        Change change = Change.builder().action(action).resourceRecordSet(recordSet).build();

        ChangeBatch batch =
                ChangeBatch.builder().changes(change).comment(action + " " + name).build();

        ChangeResourceRecordSetsResponse response =
                client.changeResourceRecordSets(
                        ChangeResourceRecordSetsRequest.builder()
                                .hostedZoneId(props.getHostedZoneId())
                                .changeBatch(batch)
                                .build());

        return response.changeInfo().id();
    }
}
