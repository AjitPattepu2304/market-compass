package com.marketcompass.investment.persistence;

import com.marketcompass.investment.model.TradeRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Profile("postgres")
public class PersistentTradingService {
    private static final long PORTFOLIO_ID = 1L;
    private final JdbcTemplate jdbc;
    private final PersistentPortfolioService portfolioService;

    public PersistentTradingService(JdbcTemplate jdbc, PersistentPortfolioService portfolioService) {
        this.jdbc = jdbc;
        this.portfolioService = portfolioService;
    }

    @Transactional
    public TradeRecord buy(String ticker, double shares, double price, String source) {
        validate(ticker, shares, price);
        String t = normalize(ticker);
        portfolioService.initialize();
        double balance = walletBalance();
        double amount = shares * price;
        if (amount > balance) throw new IllegalStateException("Insufficient balance");

        List<Map<String,Object>> rows = jdbc.queryForList("SELECT quantity, average_cost FROM holdings WHERE portfolio_id=? AND ticker=? FOR UPDATE", PORTFOLIO_ID, t);
        if (rows.isEmpty()) {
            jdbc.update("INSERT INTO holdings (portfolio_id,ticker,name,asset_type,sector,quantity,average_cost,current_price) VALUES (?,?,?,'STOCK','Unknown',?,?,?)", PORTFOLIO_ID,t,t,shares,price,price);
        } else {
            double oldQty = ((Number)rows.get(0).get("quantity")).doubleValue();
            double oldCost = ((Number)rows.get(0).get("average_cost")).doubleValue();
            double qty = oldQty + shares;
            double avg = ((oldQty * oldCost) + (shares * price)) / qty;
            jdbc.update("UPDATE holdings SET quantity=?,average_cost=?,current_price=?,updated_at=CURRENT_TIMESTAMP WHERE portfolio_id=? AND ticker=?",qty,avg,price,PORTFOLIO_ID,t);
        }
        double newBalance = balance - amount;
        jdbc.update("UPDATE paper_wallets SET available_balance=?,updated_at=CURRENT_TIMESTAMP WHERE portfolio_id=?",newBalance,PORTFOLIO_ID);
        long id = insertTrade(t,"BUY",shares,price,source,0,newBalance);
        return getTrade(id);
    }

    @Transactional
    public TradeRecord sell(String ticker, double shares, double price, String source) {
        validate(ticker, shares, price);
        String t = normalize(ticker);
        portfolioService.initialize();
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT quantity,average_cost FROM holdings WHERE portfolio_id=? AND ticker=? FOR UPDATE",PORTFOLIO_ID,t);
        if (rows.isEmpty()) throw new IllegalArgumentException("No holding found for ticker: " + t);
        double qty = ((Number)rows.get(0).get("quantity")).doubleValue();
        double avg = ((Number)rows.get(0).get("average_cost")).doubleValue();
        if (shares > qty) throw new IllegalArgumentException("Cannot sell more shares than held");
        double proceeds = shares * price;
        double gain = proceeds - (shares * avg);
        double remaining = qty - shares;
        if (remaining <= 0.000001) jdbc.update("DELETE FROM holdings WHERE portfolio_id=? AND ticker=?",PORTFOLIO_ID,t);
        else jdbc.update("UPDATE holdings SET quantity=?,current_price=?,updated_at=CURRENT_TIMESTAMP WHERE portfolio_id=? AND ticker=?",remaining,price,PORTFOLIO_ID,t);
        double newBalance = walletBalance() + proceeds;
        jdbc.update("UPDATE paper_wallets SET available_balance=?,realized_gain_loss=realized_gain_loss+?,updated_at=CURRENT_TIMESTAMP WHERE portfolio_id=?",newBalance,gain,PORTFOLIO_ID);
        long id = insertTrade(t,"SELL",shares,price,source,gain,newBalance);
        return getTrade(id);
    }

    public List<TradeRecord> history(String ticker) {
        if (ticker == null || ticker.isBlank()) return jdbc.query("SELECT * FROM trades WHERE portfolio_id=? ORDER BY executed_at DESC",this::mapTrade,PORTFOLIO_ID);
        return jdbc.query("SELECT * FROM trades WHERE portfolio_id=? AND ticker=? ORDER BY executed_at DESC",this::mapTrade,PORTFOLIO_ID,normalize(ticker));
    }

    public Map<String,Object> walletSummary() {
        portfolioService.initialize();
        double invested = jdbc.queryForObject("SELECT COALESCE(SUM(quantity*average_cost),0) FROM holdings WHERE portfolio_id=?",Double.class,PORTFOLIO_ID);
        double value = jdbc.queryForObject("SELECT COALESCE(SUM(quantity*current_price),0) FROM holdings WHERE portfolio_id=?",Double.class,PORTFOLIO_ID);
        double starting = jdbc.queryForObject("SELECT starting_balance FROM paper_wallets WHERE portfolio_id=?",Double.class,PORTFOLIO_ID);
        double balance = walletBalance();
        double realized = jdbc.queryForObject("SELECT realized_gain_loss FROM paper_wallets WHERE portfolio_id=?",Double.class,PORTFOLIO_ID);
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("startingBalance",starting); r.put("availableBalance",balance); r.put("investedAmount",invested);
        r.put("totalPortfolioValue",value); r.put("totalAccountValue",balance+value); r.put("realizedGainLoss",realized);
        r.put("totalReturnPercent",starting==0?0:((balance+value-starting)/starting)*100);
        r.put("tradeCount",jdbc.queryForObject("SELECT COUNT(*) FROM trades WHERE portfolio_id=?",Long.class,PORTFOLIO_ID));
        return r;
    }

    private double walletBalance(){ return jdbc.queryForObject("SELECT available_balance FROM paper_wallets WHERE portfolio_id=? FOR UPDATE",Double.class,PORTFOLIO_ID); }
    private long insertTrade(String ticker,String side,double shares,double price,String source,double gain,double balance){
        return jdbc.queryForObject("INSERT INTO trades (portfolio_id,ticker,side,quantity,execution_price,source,gain_loss,balance_after) VALUES (?,?,?,?,?,?,?,?) RETURNING id",Long.class,PORTFOLIO_ID,ticker,side,shares,price,source,gain,balance);
    }
    private TradeRecord getTrade(long id){ return jdbc.queryForObject("SELECT * FROM trades WHERE id=?",this::mapTrade,id); }
    private TradeRecord mapTrade(java.sql.ResultSet rs,int row) throws java.sql.SQLException{
        return TradeRecord.builder().id("TRD-"+rs.getLong("id")).side(rs.getString("side")).ticker(rs.getString("ticker"))
                .assetName(rs.getString("asset_name")==null?rs.getString("ticker"):rs.getString("asset_name"))
                .assetType(rs.getString("asset_type")).shares(rs.getDouble("quantity")).executedPrice(rs.getDouble("execution_price"))
                .gainLoss(rs.getDouble("gain_loss")).source(rs.getString("source")).balanceAfter(rs.getDouble("balance_after")).build();
    }
    private void validate(String ticker,double shares,double price){ if(ticker==null||ticker.isBlank())throw new IllegalArgumentException("Ticker is required"); if(shares<=0)throw new IllegalArgumentException("Shares must be positive"); if(price<=0)throw new IllegalArgumentException("Price must be positive"); }
    private String normalize(String ticker){return ticker.trim().toUpperCase(Locale.ROOT);}
}
