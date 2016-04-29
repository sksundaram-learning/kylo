/*
 * Copyright (c) 2016. Teradata Inc.
 */

package com.thinkbiganalytics.nifi.v2.ingest;

import com.thinkbiganalytics.nifi.core.api.metadata.MetadataProvider;
import com.thinkbiganalytics.nifi.core.api.metadata.MetadataProviderService;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ProcessorLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.*;

@EventDriven
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"thinkbig", "registration", "put"})
@CapabilityDescription("Saves the outcome of registration.")

public class UpdateRegistration extends AbstractProcessor {

    public static final String SUCCESS = "success";
    public static final String FAIL = "fail";

    // Relationships

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Registration succeeded.")
            .build();

    private final Set<Relationship> relationships;

    public static final PropertyDescriptor METADATA_SERVICE = new PropertyDescriptor.Builder()
            .name("Metadata Service")
            .description("The Think Big metadata service")
            .required(true)
            .identifiesControllerService(MetadataProviderService.class)
            .build();

    public static final PropertyDescriptor FEED_CATEGORY = new PropertyDescriptor.Builder()
            .name("System Feed Category")
            .description("System category of feed this processor supports")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor FEED_NAME = new PropertyDescriptor.Builder()
            .name("System Feed Name")
            .description("System name of feed this processor supports")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor RESULT = new PropertyDescriptor.Builder()
            .name("Result")
            .description("Indicates what should happen when a file with the same name already exists in the output directory")
            .required(true)
            .defaultValue(SUCCESS)
            .allowableValues(SUCCESS, FAIL)
            .build();

    private final List<PropertyDescriptor> propDescriptors;

    public UpdateRegistration() {
        HashSet r = new HashSet();
        r.add(REL_SUCCESS);

        this.relationships = Collections.unmodifiableSet(r);
        ArrayList pds = new ArrayList();
        pds.add(METADATA_SERVICE);
        pds.add(FEED_CATEGORY);
        pds.add(FEED_NAME);
        pds.add(RESULT);
        this.propDescriptors = Collections.unmodifiableList(pds);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propDescriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile incoming = session.get();
        FlowFile outgoing = (incoming == null ? session.create() : incoming);
        ProcessorLog logger = getLogger();

        final MetadataProviderService metadataService = context.getProperty(METADATA_SERVICE).asControllerService(MetadataProviderService.class);
        final String categoryName = context.getProperty(FEED_CATEGORY).evaluateAttributeExpressions(outgoing).getValue();
        final String feedName = context.getProperty(FEED_NAME).evaluateAttributeExpressions(outgoing).getValue();
        final String result = context.getProperty(RESULT).getValue();
        try {
            final MetadataProvider client = metadataService.getProvider();
            client.recordRegistration(categoryName, feedName, (SUCCESS.equals(result)));

        } catch (final Exception e) {
            logger.error("Failed to update registration due to {}", new Object[]{incoming, e});
        }
        session.transfer(outgoing, REL_SUCCESS);
    }

}