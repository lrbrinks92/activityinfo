package org.activityinfo.core.shared.importing.strategy;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.activityinfo.core.client.InstanceQuery;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.core.shared.Projection;
import org.activityinfo.core.shared.criteria.ClassCriteria;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.formTree.FieldPath;
import org.activityinfo.core.shared.importing.source.SourceRow;
import org.activityinfo.core.shared.importing.validation.ValidationResult;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.primitive.TextValue;
import org.activityinfo.model.formTree.FieldPath;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;


public class SingleClassImporter implements FieldImporter {

    private ResourceId rangeClassId;
    private ResourceId fieldId;

    private boolean required;
    /**
     * List of columns to match against name properties of potential reference matches.
     */
    private List<ColumnAccessor> sources;

    private List<FieldImporterColumn> fieldImporterColumns = Lists.newArrayList();

    /**
     * The list of nested text fields to match against, mapped to the
     * index of the column they are to be matched against.
     */
    private Map<FieldPath, Integer> referenceFields;

    private InstanceScoreSource scoreSource;
    private InstanceScorer instanceScorer = null;

    public SingleClassImporter(ResourceId rangeClassId,
                               boolean required,
                               List<ColumnAccessor> sourceColumns,
                               Map<FieldPath, Integer> referenceFields,
                               List<FieldImporterColumn> fieldImporterColumns,
                               ResourceId fieldId) {
        this.rangeClassId = rangeClassId;
        this.required = required;
        this.sources = sourceColumns;
        this.referenceFields = referenceFields;
        this.fieldImporterColumns = fieldImporterColumns;
        this.fieldId = fieldId;
    }

    @Override
    public Promise<Void> prepare(ResourceLocator locator, List<? extends SourceRow> batch) {

        InstanceQuery query = new InstanceQuery(
                Lists.newArrayList(referenceFields.keySet()),
                new ClassCriteria(rangeClassId));
        return locator.query(query).then(new Function<List<Projection>, Void>() {
            @Nullable
            @Override
            public Void apply(List<Projection> projections) {
                scoreSource = new InstanceScoreSourceBuilder(referenceFields, sources).build(projections);
                instanceScorer = new InstanceScorer(scoreSource);
                return null;
            }
        });
    }

    public static String[] toArray(Projection projection, Map<FieldPath, Integer> referenceFields, int arraySize) {
        String[] values = new String[arraySize];
        for (Map.Entry<FieldPath, FieldValue> entry : projection.getValueMap().entrySet()) {
            Integer index = referenceFields.get(entry.getKey());
            if (index != null) {
                Object value = entry.getValue();
                if (value instanceof TextValue) {
                    values[index] = ((TextValue) value).asString();
                }
            }
        }
        return values;
    }


    @Override
    public void validateInstance(SourceRow row, List<ValidationResult> results) {
        final InstanceScorer.Score score = instanceScorer.score(row);
        final int bestMatchIndex = score.getBestMatchIndex();

        for (int i = 0; i != sources.size(); ++i) {
            if (score.getImported()[i] == null) {
                if(required) {
                    results.add(ValidationResult.error("required missing"));
                } else {
                    results.add(ValidationResult.MISSING);
                }
            } else if (bestMatchIndex == -1) {
                results.add(ValidationResult.error("No match"));
            } else {
                String matched = scoreSource.getReferenceValues().get(bestMatchIndex)[i];
                final ValidationResult converted = ValidationResult.converted(matched, score.getBestScores()[i]);
                converted.setInstanceId(scoreSource.getReferenceInstanceIds().get(bestMatchIndex));
                results.add(converted);
            }
        }
    }

    @Override
    public boolean updateInstance(SourceRow row, FormInstance instance) {
        // root
        final List<ValidationResult> validationResults = Lists.newArrayList();
        validateInstance(row, validationResults);
        for (ValidationResult result : validationResults) {
            if (result.shouldPersist() && result.getInstanceId() != null) {
                instance.set(fieldId, result.getInstanceId());
            }
        }

        // nested data
        // todo ???
        return true;
    }

    @Override
    public List<FieldImporterColumn> getColumns() {
        return fieldImporterColumns;
    }
}
