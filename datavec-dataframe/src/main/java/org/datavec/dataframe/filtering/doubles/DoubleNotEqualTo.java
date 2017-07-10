package org.datavec.dataframe.filtering.doubles;

import org.datavec.dataframe.api.DoubleColumn;
import org.datavec.dataframe.api.Table;
import org.datavec.dataframe.columns.ColumnReference;
import org.datavec.dataframe.filtering.ColumnFilter;
import org.datavec.dataframe.util.Selection;

/**
 *
 */
public class DoubleNotEqualTo extends ColumnFilter {

    private double value;

    public DoubleNotEqualTo(ColumnReference reference, double value) {
        super(reference);
        this.value = value;
    }

    public Selection apply(Table relation) {
        DoubleColumn longColumn = (DoubleColumn) relation.column(getColumnReference().getColumnName());
        return longColumn.isNotEqualTo(value);
    }
}
