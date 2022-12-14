package cn.itcast.hotel.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public PageResult search(RequestParams params) {
        try {
            // 1.??????Request
            SearchRequest request = new SearchRequest("hotel");
            // 2.??????????????????
            // 2.1.query
            buildBasicQuery(params, request);
            // 2.2.??????
            int page = params.getPage();
            int size = params.getSize();
            request.source().from((page - 1) * size).size(size);
            // 2.3.????????????
            String location = params.getLocation();
            if (StringUtils.isNotBlank(location)) {
                request.source().sort(SortBuilders
                        .geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS));
            }
            // 3.????????????
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            // 4.????????????
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("??????????????????", e);
        }
    }

    @Override
    public Map<String, List<String>> getFilters(RequestParams params) {
        try {
            // 1.????????????
            SearchRequest request = new SearchRequest("hotel");
            // 2.????????????
            // 2.1.query
            buildBasicQuery(params, request);
            // 2.2.size
            request.source().size(0);
            // 2.3.??????
            buildAggregations(request);
            // 3.????????????
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            // 4.????????????
            Aggregations aggregations = response.getAggregations();
            Map<String, List<String>> filters = new HashMap<>(3);

            /*
             * ??????????????????,?????????????????????????????????????????????????????????.
             * ???????????????????????????,????????????????????????,put null???map?????????????????????????????????.
             */

            // 4.1.????????????
            List<String> brandList = getAggregationByName(aggregations, "brandAgg");
            filters.put("brand", brandList);
            // 4.1.????????????
            List<String> cityList = getAggregationByName(aggregations, "cityAgg");
            filters.put("city", cityList);
            // 4.1.????????????
            List<String> starList = getAggregationByName(aggregations, "starAgg");
            filters.put("starName", starList);

            return filters;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // elasticsearch,???????????????????????????????????????????????????????????????????????????????????????.
    @Override
    public List<String> getSuggestion(String key) {
        try {
            // 1.????????????
            SearchRequest request = new SearchRequest("hotel");
            // 2.????????????
            request.source().suggest(new SuggestBuilder()
                    .addSuggestion(
                            "hotelSuggest",
                            SuggestBuilders
                                    .completionSuggestion("suggestion")
                                    .size(10)
                                    .skipDuplicates(true)
                                    .prefix(key)));
            // 3.????????????
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            // 4.??????
            Suggest suggest = response.getSuggest();
            // 4.1.????????????????????????
            CompletionSuggestion suggestion = suggest.getSuggestion("hotelSuggest");
            // 4.2.??????options
            List<String> list = new ArrayList<>();
            for (CompletionSuggestion.Entry.Option option : suggestion.getOptions()) {
                // 4.3.?????????????????????
                String str = option.getText().toString();
                // 4.4.????????????
                list.add(str);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Long hotelId) {
        try {
            // 1.??????request
            DeleteRequest request = new DeleteRequest("hotel", hotelId.toString());
            // 2.????????????
            restHighLevelClient.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("????????????????????????", e);
        }
    }

    @Override
    public void saveById(Long hotelId) {
        try {
            // ?????????????????????????????????Feign????????????hotel-admin?????????id???????????????????????????????????????????????????
            Hotel hotel = getById(hotelId);
            // ??????
            HotelDoc hotelDoc = new HotelDoc(hotel);

            // 1.??????Request
            IndexRequest request = new IndexRequest("hotel").id(hotelId.toString());
            // 2.????????????
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            // 3.????????????
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("????????????????????????", e);
        }
    }

    private List<String> getAggregationByName(Aggregations aggregations, String aggName) {
        // 4.1.???????????????????????????????????????
        Terms terms = aggregations.get(aggName);
        // 4.2.??????buckets
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        // 4.3.??????
        List<String> list = new ArrayList<>(buckets.size());
        for (Terms.Bucket bucket : buckets) {
            String brandName = bucket.getKeyAsString();
            list.add(brandName);
        }
        return list;
    }

    private void buildAggregations(SearchRequest request) {
        request.source().aggregation(
                AggregationBuilders.terms("brandAgg").field("brand").size(100));
        request.source().aggregation(
                AggregationBuilders.terms("cityAgg").field("city").size(100));
        request.source().aggregation(
                AggregationBuilders.terms("starAgg").field("starName").size(100));
    }

    private void buildBasicQuery(RequestParams params, SearchRequest request) {
        // 1.??????Boolean??????
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 1.1.??????????????????match???????????????must???
        String key = params.getKey();
        if (StringUtils.isNotBlank(key)) {
            // ?????????????????????????????????
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        } else {
            // ?????????????????????
            boolQuery.must(QueryBuilders.matchAllQuery());
        }

        // 1.2.??????
        String brand = params.getBrand();
        if (StringUtils.isNotBlank(brand)) {
            boolQuery.filter(QueryBuilders.termQuery("brand", brand));
        }
        // 1.3.??????
        String city = params.getCity();
        if (StringUtils.isNotBlank(city)) {
            boolQuery.filter(QueryBuilders.termQuery("city", city));
        }
        // 1.4.??????
        String starName = params.getStarName();
        if (StringUtils.isNotBlank(starName)) {
            boolQuery.filter(QueryBuilders.termQuery("starName", starName));
        }
        // 1.5.????????????
        Integer minPrice = params.getMinPrice();
        Integer maxPrice = params.getMaxPrice();
        if (minPrice != null && maxPrice != null) {
            maxPrice = maxPrice == 0 ? Integer.MAX_VALUE : maxPrice;
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
        }

        // 2.??????????????????
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                boolQuery, // ???????????????boolQuery
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[] { // function??????
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                QueryBuilders.termQuery("isAD", true), // ????????????
                                ScoreFunctionBuilders.weightFactorFunction(10) // ????????????
                        )
                });

        // 3.??????????????????
        request.source().query(functionScoreQuery);
    }

    private PageResult handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 4.1.?????????
        long total = searchHits.getTotalHits().value;
        // 4.2.??????????????????
        SearchHit[] hits = searchHits.getHits();
        // 4.3.??????
        List<HotelDoc> hotels = new ArrayList<>(hits.length);
        for (SearchHit hit : hits) {
            // 4.4.??????source
            String json = hit.getSourceAsString();
            // 4.5.???????????????????????????
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            // 4.6.??????????????????
            // 1)????????????map
            Map<String, HighlightField> map = hit.getHighlightFields();
            if (map != null && !map.isEmpty()) {
                // 2???????????????????????????????????????
                HighlightField highlightField = map.get("name");
                if (highlightField != null) {
                    // 3?????????????????????????????????????????????1?????????
                    String hName = highlightField.getFragments()[0].toString();
                    // 4????????????????????????HotelDoc???
                    hotelDoc.setName(hName);
                }
            }
            // 4.8.????????????
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length > 0) {
                hotelDoc.setDistance(sortValues[0]);
            }

            // 4.9.????????????
            hotels.add(hotelDoc);
        }
        return new PageResult(total, hotels);
    }
}
