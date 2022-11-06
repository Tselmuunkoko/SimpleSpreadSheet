package com.spreadsheet;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

class CellAdapter extends TypeAdapter<Cell> {

    @Override
    public void write(JsonWriter writer, Cell cell) throws IOException {
        writer.beginObject();
        writer.name("value");
        writer.value(cell.getValue() > 0 ? String.valueOf(cell.getValue()) : "");
        writer.name("id");
        writer.value(cell.getId());
        writer.name("formula");
        writer.value(cell.getFormula() != null ? cell.getFormula() : "");
        writer.endObject();
    }

    @Override
    public Cell read(JsonReader jsonReader) throws IOException {
        return null;
    }
}
