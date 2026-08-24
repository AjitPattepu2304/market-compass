package com.marketcompass.investment.persistence;

import com.marketcompass.investment.model.PortfolioHolding;
import com.marketcompass.investment.service.ETFService;
import com.marketcompass.investment.service.StockService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("postgres")
public class PersistentPortfolioService {
    private static final long PORTFOLIO_ID = 1L;
    private final JdbcTemplate jdbc; private final StockService stockService; private final ETFService etfService;
    public PersistentPortfolioService(JdbcTemplate jdbc,StockService stockService,ETFService etfService){this.jdbc=jdbc;this.stockService=stockService;this.etfService=etfService;}
    @PostConstruct public void initialize(){jdbc.update("INSERT INTO portfolios (id,name,base_currency) VALUES (?,?,?) ON CONFLICT (id) DO NOTHING",PORTFOLIO_ID,"My Paper Portfolio","USD");jdbc.update("INSERT INTO paper_wallets (portfolio_id,starting_balance,available_balance) VALUES (?,?,?) ON CONFLICT (portfolio_id) DO NOTHING",PORTFOLIO_ID,25000.0,25000.0);}
    public List<PortfolioHolding> getHoldings(){return jdbc.query("SELECT ticker,name,asset_type,sector,quantity,average_cost,current_price,annual_dividend_per_share FROM holdings WHERE portfolio_id=? ORDER BY ticker",this::mapHolding,PORTFOLIO_ID);}
    public Optional<PortfolioHolding> getHoldingByTicker(String ticker){return jdbc.query("SELECT ticker,name,asset_type,sector,quantity,average_cost,current_price,annual_dividend_per_share FROM holdings WHERE portfolio_id=? AND ticker=?",this::mapHolding,PORTFOLIO_ID,normalize(ticker)).stream().findFirst();}
    @Transactional public void addHolding(PortfolioHolding input){PortfolioHolding h=enrich(input);String t=normalize(h.getTicker());jdbc.update("INSERT INTO holdings (portfolio_id,ticker,name,asset_type,sector,quantity,average_cost,current_price,annual_dividend_per_share) VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (portfolio_id,ticker) DO UPDATE SET name=EXCLUDED.name,asset_type=EXCLUDED.asset_type,sector=EXCLUDED.sector,quantity=EXCLUDED.quantity,average_cost=EXCLUDED.average_cost,current_price=EXCLUDED.current_price,annual_dividend_per_share=EXCLUDED.annual_dividend_per_share,updated_at=CURRENT_TIMESTAMP",PORTFOLIO_ID,t,h.getName(),h.getType(),h.getSector(),h.getShares(),h.getAvgCostBasis(),h.getCurrentPrice(),h.getAnnualDividendPerShare());}
    @Transactional public boolean removeHolding(String ticker){return jdbc.update("DELETE FROM holdings WHERE portfolio_id=? AND ticker=?",PORTFOLIO_ID,normalize(ticker))>0;}
    public Map<String,Object> getSummary(){List<PortfolioHolding> list=getHoldings();double invested=list.stream().mapToDouble(PortfolioHolding::getTotalCostBasis).sum(),value=list.stream().mapToDouble(PortfolioHolding::getMarketValue).sum(),gain=value-invested,dividend=list.stream().mapToDouble(PortfolioHolding::getAnnualDividendIncome).sum();Map<String,Double> sectors=list.stream().collect(Collectors.groupingBy(h->safe(h.getSector()),LinkedHashMap::new,Collectors.summingDouble(PortfolioHolding::getMarketValue)));Map<String,Double> types=list.stream().collect(Collectors.groupingBy(h->safe(h.getType()),LinkedHashMap::new,Collectors.summingDouble(PortfolioHolding::getMarketValue)));Map<String,Object> r=new LinkedHashMap<>();r.put("holdings",list);r.put("holdingsCount",list.size());r.put("totalInvested",round(invested));r.put("currentValue",round(value));r.put("gainLoss",round(gain));r.put("gainLossPercent",invested==0?0:round(gain/invested*100));r.put("annualDividendIncome",round(dividend));r.put("monthlyDividendIncome",round(dividend/12));r.put("portfolioYieldPercent",value==0?0:round(dividend/value*100));r.put("sectorAllocation",percentages(sectors,value));r.put("typeAllocation",percentages(types,value));return r;}
    private PortfolioHolding enrich(PortfolioHolding h){String t=normalize(h.getTicker());h.setTicker(t);if("ETF".equalsIgnoreCase(h.getType()))etfService.getByTicker(t).ifPresent(e->{if(blank(h.getName()))h.setName(e.getName());if(blank(h.getSector()))h.setSector(e.getCategory());if(h.getCurrentPrice()<=0)h.setCurrentPrice(e.getCurrentPrice());if(h.getAnnualDividendPerShare()<=0)h.setAnnualDividendPerShare(e.getCurrentPrice()*e.getDistributionYieldPercent()/100);});else stockService.getByTicker(t).ifPresent(s->{h.setType("STOCK");if(blank(h.getName()))h.setName(s.getCompanyName());if(blank(h.getSector()))h.setSector(s.getSector());if(h.getCurrentPrice()<=0)h.setCurrentPrice(s.getCurrentPrice());if(h.getAnnualDividendPerShare()<=0)h.setAnnualDividendPerShare(s.getAnnualDividendPerShare());});return h;}
    private PortfolioHolding mapHolding(java.sql.ResultSet rs,int row)throws java.sql.SQLException{return PortfolioHolding.builder().ticker(rs.getString("ticker")).name(rs.getString("name")).type(rs.getString("asset_type")).sector(rs.getString("sector")).shares(rs.getDouble("quantity")).avgCostBasis(rs.getDouble("average_cost")).currentPrice(rs.getDouble("current_price")).annualDividendPerShare(rs.getDouble("annual_dividend_per_share")).build();}
    private Map<String,Double> percentages(Map<String,Double> values,double total){Map<String,Double> r=new LinkedHashMap<>();values.forEach((k,v)->r.put(k,total==0?0:round(v/total*100)));return r;}
    private boolean blank(String s){return s==null||s.isBlank();}private String safe(String s){return blank(s)?"UNKNOWN":s;}private String normalize(String s){if(blank(s))throw new IllegalArgumentException("Ticker is required");return s.trim().toUpperCase(Locale.ROOT);}private double round(double v){return Math.round(v*100)/100.0;}
}
