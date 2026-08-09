package com.magentamause.cosydomainprovider.model.core;

/** Where a flagged scan stands in the human review workflow. */
public enum WatchtowerReviewStatus {
    /** Not looked at yet. The default for every ingested scan. */
    PENDING,
    /** An admin saw it and agrees it needs watching, but took no action yet. */
    ACKNOWLEDGED,
    /** An admin judged the agent's verdict a false positive. */
    DISMISSED,
    /** The subdomain was suspended or removed as a result of this finding. */
    ACTIONED
}
