package org.datavec.dataframe.filtering.times;

import org.datavec.dataframe.api.ColumnType;
import org.datavec.dataframe.api.DateTimeColumn;
import org.datavec.dataframe.api.Table;
import org.datavec.dataframe.api.TimeColumn;
import org.datavec.dataframe.columns.Column;
import org.datavec.dataframe.columns.ColumnReference;
import org.datavec.dataframe.filtering.ColumnFilter;
import org.datavec.dataframe.util.Selection;

/**
 *
 */
public class IsNoon extends ColumnFilter {

    public IsNoon(ColumnReference reference) {
        super(reference);
    }

    @Override
    public Selection apply(Table relation) {

        String name = columnReference().getColumnName();
        Column column = relation.column(name);
        ColumnType type = column.type();
        switch (type) {
            case LOCAL_TIME:
                TimeColumn timeColumn = relation.timeColumn(name);
                return timeColumn.isNoon();
            case LOCAL_DATE_TIME:
                DateTimeColumn dateTimeColumn = relation.dateTimeColumn(name);
                return dateTimeColumn.isNoon();
            default:
                throw new UnsupportedOperationException(
                                "Columns of type " + type.name() + " do not support the operation " + "isNoon() ");
        }
    }
}
