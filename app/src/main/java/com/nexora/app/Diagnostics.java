package com.nexora.app;

import java.util.ArrayList;
import java.util.List;

/** Recommendations describe observations, never infer that occupied RAM is an error. */
final class Diagnostics {
    static final class Finding {
        final String title, description, action;
        Finding(String title, String description, String action) {
            this.title = title; this.description = description; this.action = action;
        }
    }
    static List<Finding> inspect(DeviceSnapshot s) {
        List<Finding> findings = new ArrayList<>();
        if (s.thermalStatus >= 2 || (s.temperature != Integer.MIN_VALUE && s.temperature >= 400)) {
            findings.add(new Finding("Temperatura merece atenção", "O aparelho reporta calor elevado. Pause jogos e tarefas pesadas, evite sol e deixe esfriar naturalmente. O app não consegue resfriar o hardware.", null));
        }
        int available = Metrics.percent(s.freeStorage, s.totalStorage);
        if (available >= 0 && available < 10) {
            findings.add(new Finding("Pouco espaço disponível", "Menos de 10% do volume de dados está disponível. Revise arquivos e apps no gerenciador do Android; confirme pessoalmente o que excluir.", android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        }
        if (s.lowMemory) {
            findings.add(new Finding("Pressão de memória elevada", "O Android reportou pouca memória. Salve seu trabalho e feche tarefas pesadas que não precisa. Encerrar processos automaticamente pode provocar reinícios e consumo extra.", android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        }
        if (s.battery >= 0 && s.battery <= 20 && !s.powerSave && s.batteryStatus != android.os.BatteryManager.BATTERY_STATUS_CHARGING && s.batteryStatus != android.os.BatteryManager.BATTERY_STATUS_FULL) {
            findings.add(new Finding("Carga baixa, economia desligada", "Ative a economia de bateria nas configurações para reduzir consumo. Esse modo pode limitar desempenho e atividades em segundo plano.", android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS));
        }
        if (s.health != android.os.BatteryManager.BATTERY_HEALTH_UNKNOWN && s.health != android.os.BatteryManager.BATTERY_HEALTH_GOOD) {
            findings.add(new Finding("Bateria reporta um alerta", "Estado informado pelo sistema: " + s.health() + ". Se o alerta persistir, procure assistência. Software não repara defeitos físicos.", android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS));
        }
        if (s.battery < 0 || s.totalMemory <= 0 || s.totalStorage <= 0) {
            findings.add(new Finding("Algumas leituras estão indisponíveis", "O sistema não forneceu todos os dados. Leituras ausentes não são tratadas como falhas corrigidas.", null));
        }
        return findings;
    }
}
