package org.datavec.dataframe.filtering;

import org.datavec.dataframe.api.FloatColumn;
import org.datavec.dataframe.api.Table;
import org.datavec.dataframe.columns.ColumnReference;
import org.datavec.dataframe.util.Selection;

/**
 */
public class FloatLessThanOrEqualTo extends ColumnFilter {

    private float value;

    public FloatLessThanOrEqualTo(ColumnReference reference, float value) {
        super(reference);
        this.value = value;
    }

    public Selection apply(Table relation) {
        FloatColumn floatColumn = (FloatColumn) relation.column(columnReference.getColumnName());
        return floatColumn.isLessThanOrEqualTo(value);
    }
}
