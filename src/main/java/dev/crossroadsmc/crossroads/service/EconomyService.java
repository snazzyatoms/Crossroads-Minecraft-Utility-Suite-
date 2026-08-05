package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.PlayerData;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyService {
    private final CrossroadsPlugin plugin;
    private EconomyAdapter adapter;

    public EconomyService(CrossroadsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String mode = plugin.getConfig().getString("economy.mode", "money").toLowerCase(Locale.ROOT);
        if (mode.equals("aegis_claim_blocks")) {
            this.adapter = detectAegisClaimBlocks().orElseGet(NoopEconomyAdapter::new);
            return;
        }

        boolean preferCoffers = plugin.getConfig().getBoolean("economy.prefer-coffers", true);
        boolean vaultBridge = plugin.getConfig().getBoolean("economy.vault-bridge", false);
        boolean nativeEnabled = plugin.getConfig().getBoolean("economy.native.enabled", true);

        Optional<EconomyAdapter> coffers = preferCoffers ? detectCoffers() : Optional.empty();
        if (coffers.isPresent()) {
            this.adapter = coffers.get();
            return;
        }
        if (nativeEnabled) {
            this.adapter = new NativeEconomyAdapter(plugin);
            return;
        }
        if (vaultBridge) {
            this.adapter = detectVault().orElseGet(NoopEconomyAdapter::new);
            return;
        }
        this.adapter = new NoopEconomyAdapter();
    }

    public boolean isAvailable() {
        return adapter.isAvailable();
    }

    public String getProviderName() {
        return adapter.getProviderName();
    }

    public double getBalance(OfflinePlayer player) {
        return adapter.getBalance(player);
    }

    public String format(double amount) {
        return adapter.format(amount);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return adapter.has(player, amount);
    }

    public String charge(Player player, double amount, String reason) {
        if (amount <= 0.0D) {
            return null;
        }
        return adapter.withdraw(player, amount, reason);
    }

    public String deposit(OfflinePlayer player, double amount, String reason) {
        if (amount <= 0.0D) {
            return null;
        }
        return adapter.deposit(player, amount, reason);
    }

    public String setBalance(OfflinePlayer player, double amount) {
        return adapter.setBalance(player, amount);
    }

    public String transfer(Player from, OfflinePlayer to, double amount) {
        if (amount <= 0.0D) {
            return "economy.invalid-amount";
        }
        String withdrawFailure = adapter.withdraw(from, amount, "pay");
        if (withdrawFailure != null) {
            return withdrawFailure;
        }
        String depositFailure = adapter.deposit(to, amount, "pay");
        if (depositFailure != null) {
            adapter.deposit(from, amount, "pay-refund");
            return depositFailure;
        }
        return null;
    }

    public List<BalanceEntry> topBalances(int limit) {
        return adapter.topBalances(limit);
    }

    private Optional<EconomyAdapter> detectAegisClaimBlocks() {
        if (!plugin.getAegisGuardHookService().isAvailable()) {
            return Optional.empty();
        }
        return Optional.of(new AegisClaimBlocksAdapter(plugin));
    }

    private Optional<EconomyAdapter> detectCoffers() {
        try {
            Class<?> apiClass = Class.forName("com.aegisguard.coffers.api.CoffersEconomy");
            RegisteredServiceProvider<?> registration = plugin.getServer().getServicesManager().getRegistration(apiClass);
            if (registration == null || registration.getProvider() == null) {
                return Optional.empty();
            }
            return Optional.of(new CoffersEconomyAdapter(registration.getProvider(), apiClass));
        } catch (ClassNotFoundException exception) {
            return Optional.empty();
        }
    }

    private Optional<EconomyAdapter> detectVault() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = plugin.getServer().getServicesManager().getRegistration(economyClass);
            if (registration == null || registration.getProvider() == null) {
                return Optional.empty();
            }
            return Optional.of(new VaultEconomyAdapter(registration.getProvider()));
        } catch (ClassNotFoundException | NoClassDefFoundError | IllegalStateException exception) {
            return Optional.empty();
        }
    }

    public record BalanceEntry(UUID uuid, String name, double balance) {
    }

    private interface EconomyAdapter {
        boolean isAvailable();

        String getProviderName();

        double getBalance(OfflinePlayer player);

        String format(double amount);

        boolean has(OfflinePlayer player, double amount);

        String withdraw(OfflinePlayer player, double amount, String reason);

        String deposit(OfflinePlayer player, double amount, String reason);

        String setBalance(OfflinePlayer player, double amount);

        List<BalanceEntry> topBalances(int limit);
    }

    private static final class NoopEconomyAdapter implements EconomyAdapter {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public String getProviderName() {
            return "NONE";
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            return 0.0D;
        }

        @Override
        public String format(double amount) {
            return new DecimalFormat("0.00").format(amount);
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return false;
        }

        @Override
        public String withdraw(OfflinePlayer player, double amount, String reason) {
            return "economy.unavailable";
        }

        @Override
        public String deposit(OfflinePlayer player, double amount, String reason) {
            return "economy.unavailable";
        }

        @Override
        public String setBalance(OfflinePlayer player, double amount) {
            return "economy.unavailable";
        }

        @Override
        public List<BalanceEntry> topBalances(int limit) {
            return List.of();
        }
    }

    private static final class NativeEconomyAdapter implements EconomyAdapter {
        private final CrossroadsPlugin plugin;
        private final DecimalFormat format;

        private NativeEconomyAdapter(CrossroadsPlugin plugin) {
            this.plugin = plugin;
            int decimals = Math.max(0, plugin.getConfig().getInt("economy.native.decimals", 2));
            StringBuilder pattern = new StringBuilder("0");
            if (decimals > 0) {
                pattern.append('.');
                pattern.append("0".repeat(decimals));
            }
            this.format = new DecimalFormat(pattern.toString());
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getProviderName() {
            return "Crossroads";
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            PlayerData data = plugin.getPlayerDataService().get(player.getUniqueId());
            if (!data.isBalanceInitialized()) {
                double starting = plugin.getConfig().getDouble("economy.native.starting-balance", 0.0D);
                data.setBalance(starting);
                plugin.getPlayerDataService().save(player.getUniqueId());
            }
            return round(data.getBalance());
        }

        @Override
        public String format(double amount) {
            String singular = plugin.getConfig().getString("economy.native.currency-name-singular", "coin");
            String plural = plugin.getConfig().getString("economy.native.currency-name-plural", "coins");
            String rendered = format.format(round(amount));
            return rendered + " " + (Math.abs(amount - 1.0D) < 0.0000001D ? singular : plural);
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return getBalance(player) + 0.0000001D >= amount;
        }

        @Override
        public String withdraw(OfflinePlayer player, double amount, String reason) {
            if (!has(player, amount)) {
                return "economy.insufficient-funds";
            }
            PlayerData data = plugin.getPlayerDataService().get(player.getUniqueId());
            data.setBalance(round(getBalance(player) - amount));
            plugin.getPlayerDataService().save(player.getUniqueId());
            return null;
        }

        @Override
        public String deposit(OfflinePlayer player, double amount, String reason) {
            PlayerData data = plugin.getPlayerDataService().get(player.getUniqueId());
            data.setBalance(round(getBalance(player) + amount));
            plugin.getPlayerDataService().save(player.getUniqueId());
            return null;
        }

        @Override
        public String setBalance(OfflinePlayer player, double amount) {
            PlayerData data = plugin.getPlayerDataService().get(player.getUniqueId());
            data.setBalance(round(Math.max(0.0D, amount)));
            plugin.getPlayerDataService().save(player.getUniqueId());
            return null;
        }

        @Override
        public List<BalanceEntry> topBalances(int limit) {
            List<BalanceEntry> entries = new ArrayList<>();
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                if (offlinePlayer.getUniqueId() == null) {
                    continue;
                }
                PlayerData data = plugin.getPlayerDataService().get(offlinePlayer.getUniqueId());
                if (!data.isBalanceInitialized() && data.getBalance() <= 0.0D) {
                    continue;
                }
                entries.add(new BalanceEntry(
                    offlinePlayer.getUniqueId(),
                    offlinePlayer.getName() == null ? offlinePlayer.getUniqueId().toString() : offlinePlayer.getName(),
                    getBalance(offlinePlayer)
                ));
            }
            entries.sort(Comparator.comparingDouble(BalanceEntry::balance).reversed());
            if (entries.size() > limit) {
                return entries.subList(0, limit);
            }
            return entries;
        }

        private double round(double amount) {
            int decimals = Math.max(0, plugin.getConfig().getInt("economy.native.decimals", 2));
            return BigDecimal.valueOf(amount).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
        }
    }

    private static final class AegisClaimBlocksAdapter implements EconomyAdapter {
        private final CrossroadsPlugin plugin;

        private AegisClaimBlocksAdapter(CrossroadsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean isAvailable() {
            return plugin.getAegisGuardHookService().isAvailable();
        }

        @Override
        public String getProviderName() {
            return "AegisGuard ClaimBlocks";
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            return plugin.getAegisGuardHookService().getAvailableClaimBlocks(player.getUniqueId());
        }

        @Override
        public String format(double amount) {
            long rounded = Math.round(amount);
            return rounded + " ClaimBlocks";
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return getBalance(player) >= Math.round(amount);
        }

        @Override
        public String withdraw(OfflinePlayer player, double amount, String reason) {
            long rounded = Math.max(1L, Math.round(amount));
            if (getBalance(player) < rounded) {
                return "economy.insufficient-claimblocks";
            }
            if (!plugin.getAegisGuardHookService().spendClaimBlocks(player.getUniqueId(), rounded)) {
                return "economy.claimblocks-rejected";
            }
            return null;
        }

        @Override
        public String deposit(OfflinePlayer player, double amount, String reason) {
            return "economy.claimblocks-deposit-unsupported";
        }

        @Override
        public String setBalance(OfflinePlayer player, double amount) {
            return "economy.claimblocks-set-unsupported";
        }

        @Override
        public List<BalanceEntry> topBalances(int limit) {
            return List.of();
        }
    }

    /**
     * Reflective Vault economy adapter so EconomyService itself has no hard Vault type refs.
     */
    private static final class VaultEconomyAdapter implements EconomyAdapter {
        private final Object economy;
        private final Method nameMethod;
        private final Method balanceMethod;
        private final Method formatMethod;
        private final Method hasMethod;
        private final Method withdrawMethod;
        private final Method depositMethod;
        private final Method successMethod;

        private VaultEconomyAdapter(Object economy) {
            this.economy = economy;
            try {
                Class<?> api = Class.forName("net.milkbowl.vault.economy.Economy");
                this.nameMethod = api.getMethod("getName");
                this.balanceMethod = api.getMethod("getBalance", OfflinePlayer.class);
                this.formatMethod = api.getMethod("format", double.class);
                this.hasMethod = api.getMethod("has", OfflinePlayer.class, double.class);
                this.withdrawMethod = api.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                this.depositMethod = api.getMethod("depositPlayer", OfflinePlayer.class, double.class);
                this.successMethod = withdrawMethod.getReturnType().getMethod("transactionSuccess");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to bind Vault economy API.", exception);
            }
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getProviderName() {
            try {
                return String.valueOf(nameMethod.invoke(economy));
            } catch (ReflectiveOperationException exception) {
                return "Vault";
            }
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            try {
                Object result = balanceMethod.invoke(economy, player);
                return result instanceof Number number ? number.doubleValue() : 0.0D;
            } catch (ReflectiveOperationException exception) {
                return 0.0D;
            }
        }

        @Override
        public String format(double amount) {
            try {
                return String.valueOf(formatMethod.invoke(economy, amount));
            } catch (ReflectiveOperationException exception) {
                return new DecimalFormat("0.00").format(amount);
            }
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            try {
                return Boolean.TRUE.equals(hasMethod.invoke(economy, player, amount));
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }

        @Override
        public String withdraw(OfflinePlayer player, double amount, String reason) {
            if (!has(player, amount)) {
                return "economy.insufficient-funds";
            }
            try {
                Object response = withdrawMethod.invoke(economy, player, amount);
                if (!Boolean.TRUE.equals(successMethod.invoke(response))) {
                    return "economy.transaction-rejected";
                }
                return null;
            } catch (ReflectiveOperationException exception) {
                return "economy.transaction-rejected";
            }
        }

        @Override
        public String deposit(OfflinePlayer player, double amount, String reason) {
            try {
                Object response = depositMethod.invoke(economy, player, amount);
                if (!Boolean.TRUE.equals(successMethod.invoke(response))) {
                    return "economy.transaction-rejected";
                }
                return null;
            } catch (ReflectiveOperationException exception) {
                return "economy.transaction-rejected";
            }
        }

        @Override
        public String setBalance(OfflinePlayer player, double amount) {
            double current = getBalance(player);
            if (amount > current) {
                return deposit(player, amount - current, "set");
            }
            if (amount < current) {
                return withdraw(player, current - amount, "set");
            }
            return null;
        }

        @Override
        public List<BalanceEntry> topBalances(int limit) {
            return List.of();
        }
    }

    private static final class CoffersEconomyAdapter implements EconomyAdapter {
        private final Object provider;
        private final Method hasAccount;
        private final Method createAccount;
        private final Method getBalance;
        private final Method format;
        private final Method withdraw;
        private final Method deposit;

        private CoffersEconomyAdapter(Object provider, Class<?> apiClass) {
            this.provider = provider;
            try {
                this.hasAccount = apiClass.getMethod("hasAccount", java.util.UUID.class);
                this.createAccount = apiClass.getMethod("createAccount", java.util.UUID.class);
                this.getBalance = apiClass.getMethod("getBalance", java.util.UUID.class);
                this.format = apiClass.getMethod("format", BigDecimal.class);
                this.withdraw = apiClass.getMethod("withdraw", java.util.UUID.class, BigDecimal.class, String.class);
                Method depositMethod;
                try {
                    depositMethod = apiClass.getMethod("deposit", java.util.UUID.class, BigDecimal.class, String.class);
                } catch (NoSuchMethodException exception) {
                    depositMethod = null;
                }
                this.deposit = depositMethod;
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Coffers API surface changed unexpectedly.", exception);
            }
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getProviderName() {
            return "Coffers";
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            ensureAccount(player);
            try {
                Object result = getBalance.invoke(provider, player.getUniqueId());
                return result instanceof BigDecimal bigDecimal ? bigDecimal.doubleValue() : 0.0D;
            } catch (IllegalAccessException | InvocationTargetException exception) {
                return 0.0D;
            }
        }

        @Override
        public String format(double amount) {
            try {
                Object result = format.invoke(provider, BigDecimal.valueOf(amount));
                return String.valueOf(result);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                return new DecimalFormat("0.00").format(amount);
            }
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return getBalance(player) >= amount;
        }

        @Override
        public String withdraw(OfflinePlayer player, double amount, String reason) {
            ensureAccount(player);
            try {
                Object result = withdraw.invoke(provider, player.getUniqueId(), BigDecimal.valueOf(amount), reason);
                return interpretResult(result);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                return "economy.coffers-failed";
            }
        }

        @Override
        public String deposit(OfflinePlayer player, double amount, String reason) {
            ensureAccount(player);
            if (deposit == null) {
                return "economy.coffers-deposit-unsupported";
            }
            try {
                Object result = deposit.invoke(provider, player.getUniqueId(), BigDecimal.valueOf(amount), reason);
                return interpretResult(result);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                return "economy.coffers-failed";
            }
        }

        @Override
        public String setBalance(OfflinePlayer player, double amount) {
            double current = getBalance(player);
            if (amount > current) {
                return deposit(player, amount - current, "set");
            }
            if (amount < current) {
                return withdraw(player, current - amount, "set");
            }
            return null;
        }

        @Override
        public List<BalanceEntry> topBalances(int limit) {
            return List.of();
        }

        private String interpretResult(Object result) throws IllegalAccessException, InvocationTargetException {
            try {
                Method successful = result.getClass().getMethod("successful");
                boolean ok = Boolean.TRUE.equals(successful.invoke(result));
                if (ok) {
                    return null;
                }
                Method message = result.getClass().getMethod("message");
                return String.valueOf(message.invoke(result));
            } catch (NoSuchMethodException exception) {
                return "economy.coffers-failed";
            }
        }

        private void ensureAccount(OfflinePlayer player) {
            try {
                boolean exists = Boolean.TRUE.equals(hasAccount.invoke(provider, player.getUniqueId()));
                if (!exists) {
                    createAccount.invoke(provider, player.getUniqueId());
                }
            } catch (IllegalAccessException | InvocationTargetException exception) {
                // Ignore and let the next call fail gracefully.
            }
        }
    }
}
