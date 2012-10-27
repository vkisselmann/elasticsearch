/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.mapper.internal;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DeletionAwareConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.lucene.search.TermFilter;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.index.mapper.core.AbstractFieldMapper;
import org.elasticsearch.index.query.QueryParseContext;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.index.mapper.MapperBuilders.type;
import static org.elasticsearch.index.mapper.core.TypeParsers.parseField;

/**
 *
 */
public class TypeFieldMapper extends AbstractFieldMapper<String> implements InternalMapper, RootMapper {

    public static final String NAME = "_type";

    public static final Term TERM_FACTORY = new Term(NAME, "");

    public static final String CONTENT_TYPE = "_type";

    public static class Defaults extends AbstractFieldMapper.Defaults {
        public static final String NAME = TypeFieldMapper.NAME;
        public static final String INDEX_NAME = TypeFieldMapper.NAME;

        public static final FieldType TYPE_FIELD_TYPE = new FieldType(AbstractFieldMapper.Defaults.FIELD_TYPE);

        static {
            TYPE_FIELD_TYPE.setIndexed(true);
            TYPE_FIELD_TYPE.setTokenized(false);
            TYPE_FIELD_TYPE.setStored(false);
            TYPE_FIELD_TYPE.setOmitNorms(true);
            TYPE_FIELD_TYPE.setIndexOptions(IndexOptions.DOCS_ONLY);
            TYPE_FIELD_TYPE.freeze();
        }
    }

    public static class Builder extends AbstractFieldMapper.Builder<Builder, TypeFieldMapper> {

        public Builder() {
            super(Defaults.NAME, new FieldType(Defaults.TYPE_FIELD_TYPE));
            indexName = Defaults.INDEX_NAME;
        }

        @Override
        public TypeFieldMapper build(BuilderContext context) {
            return new TypeFieldMapper(name, indexName, boost, fieldType);
        }
    }

    public static class TypeParser implements Mapper.TypeParser {
        @Override
        public Mapper.Builder parse(String name, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException {
            TypeFieldMapper.Builder builder = type();
            parseField(builder, builder.name, node, parserContext);
            return builder;
        }
    }


    public TypeFieldMapper() {
        this(Defaults.NAME, Defaults.INDEX_NAME);
    }

    protected TypeFieldMapper(String name, String indexName) {
        this(name, indexName, Defaults.BOOST, new FieldType(Defaults.TYPE_FIELD_TYPE));
    }

    public TypeFieldMapper(String name, String indexName, float boost, FieldType fieldType) {
        super(new Names(name, indexName, indexName, name), boost, fieldType, Lucene.KEYWORD_ANALYZER,
                Lucene.KEYWORD_ANALYZER);
    }

    public String value(Document document) {
        Field field = (Field) document.getField(names.indexName());
        return field == null ? null : value(field);
    }

    @Override
    public String value(Field field) {
        return field.stringValue();
    }

    @Override
    public String valueFromString(String value) {
        return value;
    }

    @Override
    public String valueAsString(Field field) {
        return value(field);
    }

    @Override
    public String indexedValue(String value) {
        return value;
    }

    public Term term(String value) {
        return names().createIndexNameTerm(value);
    }

    @Override
    public Filter fieldFilter(String value, @Nullable QueryParseContext context) {
        if (!indexed()) {
            return new PrefixFilter(new Term(UidFieldMapper.NAME, Uid.typePrefix(value)));
        }
        return new TermFilter(names().createIndexNameTerm(value));
    }

    @Override
    public Query fieldQuery(String value, @Nullable QueryParseContext context) {
        return new DeletionAwareConstantScoreQuery(context.cacheFilter(fieldFilter(value, context), null));
    }

    @Override
    public boolean useFieldQueryWithQueryString() {
        return true;
    }

    @Override
    public void preParse(ParseContext context) throws IOException {
        super.parse(context);
    }

    @Override
    public void postParse(ParseContext context) throws IOException {
    }

    @Override
    public void parse(ParseContext context) throws IOException {
        // we parse in pre parse
    }

    @Override
    public void validate(ParseContext context) throws MapperParsingException {
    }

    @Override
    public boolean includeInObject() {
        return false;
    }

    @Override
    protected Field parseCreateField(ParseContext context) throws IOException {
        if (!indexed() && !stored()) {
            return null;
        }
        return new Field(names.indexName(), context.type(), fieldType);
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        // if all are defaults, no sense to write it at all
        if (stored() == Defaults.TYPE_FIELD_TYPE.stored() && indexed() == Defaults.TYPE_FIELD_TYPE.indexed()) {
            return builder;
        }
        builder.startObject(CONTENT_TYPE);
        if (stored() != Defaults.TYPE_FIELD_TYPE.stored()) {
            builder.field("store", stored());
        }
        if (indexed() != Defaults.TYPE_FIELD_TYPE.indexed()) {
            builder.field("index", indexed());
        }
        builder.endObject();
        return builder;
    }

    @Override
    public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
        // do nothing here, no merging, but also no exception
    }
}
