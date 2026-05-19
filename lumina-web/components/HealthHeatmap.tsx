import React, { useState } from 'react';
import { useLanguage } from './LanguageContext';
import { HealthHeatmapCell } from '../services/dashboardService';

interface HealthHeatmapProps {
  cells: HealthHeatmapCell[];
  days: number;
  overallSuccessRate: number;
}

const getCellColor = (cell: HealthHeatmapCell, index: number, totalCells: number): string => {
  if (cell.totalRequests === 0) {
    return 'bg-[#e8e8e8] dark:bg-gray-700/50';
  }
  const progress = index / Math.max(totalCells - 1, 1);
  const rate = cell.successRate;
  if (rate >= 99) {
    if (progress < 0.3) return 'bg-[#c6e48b] dark:bg-[#4a8c3f]';
    if (progress < 0.7) return 'bg-[#7bc96f] dark:bg-[#3b8132]';
    return 'bg-[#239a3b] dark:bg-[#2ea043]';
  }
  if (rate >= 95) {
    return 'bg-[#c6e48b] dark:bg-[#4a8c3f]';
  }
  if (rate >= 85) {
    return 'bg-[#f5d547] dark:bg-[#b8860b]';
  }
  if (rate >= 70) {
    return 'bg-[#f5a623] dark:bg-[#d2691e]';
  }
  return 'bg-[#e74c3c] dark:bg-[#c0392b]';
};

const formatTime = (ts: number): string => {
  const d = new Date(ts);
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hour = String(d.getHours()).padStart(2, '0');
  const min = String(d.getMinutes()).padStart(2, '0');
  return `${month}/${day} ${hour}:${min}`;
};

export const HealthHeatmap: React.FC<HealthHeatmapProps> = ({ cells, days, overallSuccessRate }) => {
  const { t } = useLanguage();
  const [tooltip, setTooltip] = useState<{
    x: number;
    y: number;
    cell: HealthHeatmapCell;
    endTs: number;
  } | null>(null);

  const rows = 6;
  const cols = Math.ceil(cells.length / rows);

  const handleMouseEnter = (e: React.MouseEvent, cell: HealthHeatmapCell, idx: number) => {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const container = (e.currentTarget as HTMLElement).closest('[data-heatmap-container]');
    const containerRect = container?.getBoundingClientRect() || rect;
    const endTs = idx < cells.length - 1 ? cells[idx + 1].timestamp : cell.timestamp + 900000;
    setTooltip({
      x: rect.left - containerRect.left + rect.width / 2,
      y: rect.top - containerRect.top,
      cell,
      endTs,
    });
  };

  const rateColor = overallSuccessRate >= 99
    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
    : overallSuccessRate >= 95
      ? 'bg-lime-100 text-lime-700 dark:bg-lime-900/30 dark:text-lime-300'
      : overallSuccessRate >= 85
        ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-300'
        : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300';

  return (
    <div className="relative" data-heatmap-container>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-base font-bold text-gray-900 dark:text-white">
          {t('dashboard.healthHeatmap.title')}
        </h3>
        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-500 dark:text-gray-400">
            {t('dashboard.healthHeatmap.days', { days })}
          </span>
          <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${rateColor}`}>
            {overallSuccessRate.toFixed(1)}%
          </span>
        </div>
      </div>

      <div
        className="grid gap-[3px]"
        style={{ gridTemplateColumns: `repeat(${cols}, 1fr)` }}
        onMouseLeave={() => setTooltip(null)}
      >
        {Array.from({ length: cols * rows }).map((_, idx) => {
          const colIdx = idx % cols;
          const rowIdx = Math.floor(idx / cols);
          const cellIdx = colIdx * rows + rowIdx;
          const cell = cells[cellIdx];

          if (!cell) {
            return <div key={idx} className="aspect-square rounded-[3px] bg-[#ebedf0] dark:bg-gray-700/40" />;
          }

          return (
            <div
              key={idx}
              className={`aspect-square rounded-[3px] cursor-pointer hover:ring-1 hover:ring-gray-900/20 dark:hover:ring-white/30 hover:z-10 ${getCellColor(cell, cellIdx, cells.length)}`}
              onMouseEnter={(e) => handleMouseEnter(e, cell, cellIdx)}
            />
          );
        })}
      </div>

      {tooltip && (
        <div
          className="absolute z-50 pointer-events-none bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg px-3 py-2 text-xs whitespace-nowrap"
          style={{
            left: `clamp(80px, ${tooltip.x}px, calc(100% - 80px))`,
            top: tooltip.y - 6,
            transform: 'translate(-50%, -100%)',
          }}
        >
          <div className="text-gray-500 dark:text-gray-400 mb-1">
            {t('dashboard.healthHeatmap.tooltip.time', {
              start: formatTime(tooltip.cell.timestamp),
              end: formatTime(tooltip.endTs),
            })}
          </div>
          <div className="flex items-center gap-3">
            <span className="text-emerald-600 dark:text-emerald-400 font-medium">
              ✓ {tooltip.cell.successRequests}
            </span>
            <span className="text-red-600 dark:text-red-400 font-medium">
              ✗ {tooltip.cell.totalRequests - tooltip.cell.successRequests}
            </span>
            {tooltip.cell.totalRequests > 0 && (
              <span className="text-gray-500 dark:text-gray-400">
                ({tooltip.cell.successRate.toFixed(1)}%)
              </span>
            )}
          </div>
        </div>
      )}

      <div className="flex items-center justify-center gap-2 mt-3 text-[11px] text-gray-400 dark:text-gray-500">
        <span>{t('dashboard.healthHeatmap.legend.earliest')}</span>
        <div className="flex gap-[3px]">
          <div className="w-[10px] h-[10px] rounded-[2px] bg-[#ebedf0] dark:bg-gray-700/40" />
          <div className="w-[10px] h-[10px] rounded-[2px] bg-[#e74c3c]" />
          <div className="w-[10px] h-[10px] rounded-[2px] bg-[#f5a623]" />
          <div className="w-[10px] h-[10px] rounded-[2px] bg-[#f5d547]" />
          <div className="w-[10px] h-[10px] rounded-[2px] bg-[#239a3b]" />
        </div>
        <span>{t('dashboard.healthHeatmap.legend.latest')}</span>
      </div>
    </div>
  );
};
