package com.scitequest.martin.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

public final class GenericTable extends JFrame {
    private final JTable table;
    private final TableModel model;
    private final List<Column<?>> columns;

    public GenericTable() {
        super();

        columns = new ArrayList<>();
        model = new TableModel();
        table = new JTable(model);

        // Enable sorting
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Style improvements
        table.setShowGrid(true);
        table.setGridColor(UIManager.getColor("Table.gridColor"));
        table.getTableHeader().setReorderingAllowed(false);

        // Add table to scrollpane
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void add(Column<?> column) {
        columns.add(column);
        model.fireTableStructureChanged();
    }

    private class TableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return columns.isEmpty() ? 0 : columns.get(0).size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column).getName();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columns.get(columnIndex).getType();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns.get(columnIndex).get(rowIndex);
        }
    }
}

abstract class Column<T> {
    private final String name;
    private final List<T> values;

    protected Column(String name) {
        this.name = name;
        this.values = new ArrayList<>();
    }

    public void add(T value) {
        values.add(value);
    }

    public T get(int index) {
        return values.get(index);
    }

    public String getName() {
        return name;
    }

    public int size() {
        return values.size();
    }

    public abstract Class<T> getType();
}

class IntColumn extends Column<Integer> {
    public IntColumn(String name) {
        super(name);
    }

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }
}

class DoubleColumn extends Column<Double> {
    public DoubleColumn(String name) {
        super(name);
    }

    @Override
    public Class<Double> getType() {
        return Double.class;
    }
}
