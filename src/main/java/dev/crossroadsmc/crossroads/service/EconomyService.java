package dev.crossroadsmc.crossroads.service;

import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Optional;
import net.milkbowl.vault.economy.Economy;
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
        this.adapter = detectCoffers().or(this::detectVault).orElseGet(NoopEconomyAdapter::new);
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
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            return Optional.empty();
        }
        return Optional.of(new VaultEconomyAdapter(registration.getProvider()));
    }

    private interface EconomyAdapter {
        boolean isAvailable();

        String getProviderName();

        double getBalance(OfflinePlayer player);

        String format(double amount);

        boolean has(OfflinePlayer player, double amount);

        String withdraw(OfflinePlayer player, double amount, String reason);
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
            return "No compatible economy provider is active.";
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
                return "Insufficient AegisGuard ClaimBlocks.";
            }
            if (!plugin.getAegisGuardHookService().spendClaimBlocks(player.getUniqueId(), rounded)) {
                return "AegisGuard rejected the ClaimBlocks transaction.";
            }
            return null;
        }
    }

    private static final class VaultEconomyAdapter implements EconomyAdapter {
        private final Economy economy;

        private VaultEconomyAdapter(Economy economy) {
            this.economy = economy;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getProviderName() {
            return economy.getName();
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            return economy.getBalance(player);
        }

        @Override
        public String format(double amount) {
            return economy.format(amount);
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return economy.has(player, amount);
        }

        @Override
        public String withdraw(OfflinePlayer player, double amount, String reason) {
            if (!economy.has(player, amount)) {
                return "Insufficient funds.";
            }
            if (!economy.withdrawPlayer(player, amount).transactionSuccess()) {
                return "The economy provider rejected the transaction.";
            }
            return null;
        }
    }

    private static final class CoffersEconomyAdapter implements EconomyAdapter {
        private final Object provider;
        private final Method hasAccount;
        private final Method createAccount;
        private final Method getBalance;
        private final Method format;
        private final Method withdraw;

        private CoffersEconomyAdapter(Object provider, Class<?> apiClass) {
            this.provider = provider;
            try {
                this.hasAccount = apiClass.getMethod("hasAccount", java.util.UUID.class);
                this.createAccount = apiClass.getMethod("createAccount", java.util.UUID.class);
                this.getBalance = apiClass.getMethod("getBalance", java.util.UUID.class);
                this.format = apiClass.getMethod("format", BigDecimal.class);
                this.withdraw = apiClass.getMethod("withdraw", java.util.UUID.class, BigDecimal.class, String.class);
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
                Method successful = result.getClass().getMethod("successful");
                boolean ok = Boolean.TRUE.equals(successful.invoke(result));
                if (ok) {
                    return null;
                }
                Method message = result.getClass().getMethod("message");
                return String.valueOf(message.invoke(result));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                return "The Coffers transaction failed.";
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
