import React, { useEffect, useState } from 'react';
import { Plus, Trash2, Copy, Check, X, Loader2, BarChart3, ToggleLeft, ToggleRight, ChevronDown, ChevronRight, PencilLine, RotateCcw } from 'lucide-react';
import { useLanguage } from './LanguageContext';
import { tokenService } from '../services/tokenService';
import { AccessToken, Group } from '../types';
import { groupService } from '../services/groupService';
import { SlideInItem } from './Animations';
import { Button } from './ui/Button';
import { Modal } from './ui/Modal';
import { useToast } from './ui/ToastContext';
import { Pagination } from './Pagination';

const PAGE_SIZE = 10;
const DEFAULT_KEY_GROUP = '自用';

export const Tokens: React.FC = () => {
  const { t } = useLanguage();
  const { showToast } = useToast();

  const [activeTokens, setActiveTokens] = useState<AccessToken[]>([]);
  const [disabledTokens, setDisabledTokens] = useState<AccessToken[]>([]);
  const [isActiveLoading, setIsActiveLoading] = useState(true);
  const [isDisabledLoading, setIsDisabledLoading] = useState(false);
  const [activePage, setActivePage] = useState(1);
  const [disabledPage, setDisabledPage] = useState(1);
  const [activeTotal, setActiveTotal] = useState(0);
  const [disabledTotal, setDisabledTotal] = useState(0);
  const [isCreatingToken, setIsCreatingToken] = useState(false);
  const [newTokenName, setNewTokenName] = useState('');
  const [newTokenGroup, setNewTokenGroup] = useState(DEFAULT_KEY_GROUP);
  const [createdToken, setCreatedToken] = useState<AccessToken | null>(null);
  const [copyFeedbackId, setCopyFeedbackId] = useState<string | null>(null);
  const [revokeId, setRevokeId] = useState<string | null>(null);
  const [showDisabled, setShowDisabled] = useState(false);
  const [quotaEditId, setQuotaEditId] = useState<string | null>(null);
  const [quotaInputs, setQuotaInputs] = useState<Record<string, string>>({});
  const [requestLimitInputs, setRequestLimitInputs] = useState<Record<string, string>>({});
  const [concurrencyLimitInputs, setConcurrencyLimitInputs] = useState<Record<string, string>>({});
  const [modelLimitInputs, setModelLimitInputs] = useState<Record<string, string[]>>({});
  const [groupInputs, setGroupInputs] = useState<Record<string, string>>({});
  const [keyGroups, setKeyGroups] = useState<string[]>([DEFAULT_KEY_GROUP]);
  const [selectedGroup, setSelectedGroup] = useState(DEFAULT_KEY_GROUP);
  const [keyword, setKeyword] = useState('');
  const [modelOptions, setModelOptions] = useState<Group[]>([]);
  const [savingQuotaId, setSavingQuotaId] = useState<string | null>(null);
  const [resettingRequestLimitId, setResettingRequestLimitId] = useState<string | null>(null);
  const [resetConfirmToken, setResetConfirmToken] = useState<AccessToken | null>(null);

  useEffect(() => {
    fetchActiveTokens(activePage);
  }, [activePage, selectedGroup, keyword]);

  useEffect(() => {
    fetchModelOptions();
  }, []);

  useEffect(() => {
    fetchDisabledTokens(disabledPage);
  }, [disabledPage, selectedGroup, keyword]);

  useEffect(() => {
    fetchKeyGroups();
  }, []);

  const fetchActiveTokens = async (page = activePage) => {
    setIsActiveLoading(true);
    try {
      const data = await tokenService.getPage(page, PAGE_SIZE, true, selectedGroup, keyword);
      setActiveTokens(data.items);
      setActiveTotal(data.total);
    } catch (error) {
      console.error('Failed to fetch active tokens', error);
    } finally {
      setIsActiveLoading(false);
    }
  };

  const fetchDisabledTokens = async (page = disabledPage) => {
    setIsDisabledLoading(true);
    try {
      const data = await tokenService.getPage(page, PAGE_SIZE, false, selectedGroup, keyword);
      setDisabledTokens(data.items);
      setDisabledTotal(data.total);
    } catch (error) {
      console.error('Failed to fetch disabled tokens', error);
    } finally {
      setIsDisabledLoading(false);
    }
  };

  const fetchModelOptions = async () => {
    try {
      const groups = await groupService.getList(1, 1000);
      setModelOptions(groups);
    } catch (error) {
      console.error('Failed to fetch model groups', error);
    }
  };

  const fetchKeyGroups = async () => {
    try {
      const groups = await tokenService.getGroups();
      const normalized = Array.from(new Set([DEFAULT_KEY_GROUP, ...groups.filter(Boolean)]));
      setKeyGroups(normalized);
      if (!normalized.includes(selectedGroup)) {
        setSelectedGroup(DEFAULT_KEY_GROUP);
      }
    } catch (error) {
      console.error('Failed to fetch token groups', error);
    }
  };

  const refreshTokenPages = () => {
    fetchActiveTokens(activePage);
    if (showDisabled) {
      fetchDisabledTokens(disabledPage);
    }
  };

  const handleCreateToken = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTokenName.trim()) return;
    setIsCreatingToken(true);
    try {
      const newToken = await tokenService.create(newTokenName, newTokenGroup.trim() || DEFAULT_KEY_GROUP);
      setCreatedToken(newToken);
      setActivePage(1);
      setSelectedGroup(newToken.keyGroup || DEFAULT_KEY_GROUP);
      fetchKeyGroups();
      fetchActiveTokens(1);
    } catch (error) {
      console.error('Failed to create token', error);
      showToast(t('common.fail'), 'error');
    } finally {
      setIsCreatingToken(false);
      setNewTokenName('');
      setNewTokenGroup(DEFAULT_KEY_GROUP);
    }
  };
  const handleToggleToken = async (id: string) => {
    try {
      await tokenService.toggle(id);
      refreshTokenPages();
      showToast(t('common.success'), 'success');
    } catch (error) {
      console.error('Failed to toggle token', error);
      showToast(t('common.fail'), 'error');
    }
  };

  const handleRevokeToken = async (id: string) => {
    try {
      await tokenService.delete(id);
      setRevokeId(null);
      refreshTokenPages();
      showToast(t('settings.tokens.revokeSuccess'), 'success');
    } catch (error) {
      console.error('Failed to revoke token', error);
      showToast(t('common.fail'), 'error');
    }
  };

  const startQuotaEdit = (token: AccessToken) => {
    setQuotaEditId(token.id);
    setQuotaInputs((current) => ({
      ...current,
      [token.id]: token.maxAmount == null ? '' : String(token.maxAmount),
    }));
    setRequestLimitInputs((current) => ({
      ...current,
      [token.id]: token.maxRequests == null ? '' : String(token.maxRequests),
    }));
    setConcurrencyLimitInputs((current) => ({
      ...current,
      [token.id]: token.maxConcurrentRequests == null ? '' : String(token.maxConcurrentRequests),
    }));
    setModelLimitInputs((current) => ({
      ...current,
      [token.id]: token.supportedModels || [],
    }));
    setGroupInputs((current) => ({
      ...current,
      [token.id]: token.keyGroup || DEFAULT_KEY_GROUP,
    }));
  };

  const cancelQuotaEdit = () => {
    setQuotaEditId(null);
  };

  const handleQuotaInputChange = (id: string, value: string) => {
    setQuotaInputs((current) => ({ ...current, [id]: value }));
  };

  const handleRequestLimitInputChange = (id: string, value: string) => {
    setRequestLimitInputs((current) => ({ ...current, [id]: value }));
  };

  const handleConcurrencyLimitInputChange = (id: string, value: string) => {
    setConcurrencyLimitInputs((current) => ({ ...current, [id]: value }));
  };

  const handleModelLimitToggle = (id: string, modelName: string) => {
    setModelLimitInputs((current) => {
      const selected = current[id] || [];
      const next = selected.includes(modelName)
        ? selected.filter((item) => item !== modelName)
        : [...selected, modelName];
      return { ...current, [id]: next };
    });
  };

  const handleModelLimitClear = (id: string) => {
    setModelLimitInputs((current) => ({ ...current, [id]: [] }));
  };

  const handleGroupInputChange = (id: string, value: string) => {
    setGroupInputs((current) => ({ ...current, [id]: value }));
  };

  const handleQuotaSave = async (id: string) => {
    const rawValue = (quotaInputs[id] || '').trim();
    const maxAmount = rawValue === '' ? null : Number(rawValue);
    if (maxAmount !== null && (!Number.isFinite(maxAmount) || maxAmount < 0)) {
      showToast(t('settings.tokens.invalidQuota'), 'error');
      return;
    }

    const rawRequestLimit = (requestLimitInputs[id] || '').trim();
    const maxRequests = rawRequestLimit === '' ? null : Number(rawRequestLimit);
    if (
      maxRequests !== null &&
      (!Number.isInteger(maxRequests) || maxRequests < 0)
    ) {
      showToast(t('settings.tokens.invalidRequestLimit'), 'error');
      return;
    }

    const rawConcurrencyLimit = (concurrencyLimitInputs[id] || '').trim();
    const maxConcurrentRequests = rawConcurrencyLimit === '' ? null : Number(rawConcurrencyLimit);
    if (
      maxConcurrentRequests !== null &&
      (!Number.isInteger(maxConcurrentRequests) || maxConcurrentRequests < 0)
    ) {
      showToast(t('settings.tokens.invalidConcurrencyLimit'), 'error');
      return;
    }

    setSavingQuotaId(id);
    try {
      await tokenService.updateQuota(
        id,
        maxAmount,
        maxRequests,
        maxConcurrentRequests,
        modelLimitInputs[id] || [],
        (groupInputs[id] || '').trim() || DEFAULT_KEY_GROUP
      );
      setQuotaEditId(null);
      fetchKeyGroups();
      refreshTokenPages();
      showToast(t('settings.tokens.quotaUpdated'), 'success');
    } catch (error) {
      console.error('Failed to update token quota', error);
      showToast(t('common.fail'), 'error');
    } finally {
      setSavingQuotaId(null);
    }
  };

  const handleResetRequestLimit = async (id: string) => {
    setResettingRequestLimitId(id);
    try {
      await tokenService.resetRequestLimit(id);
      setResetConfirmToken(null);
      refreshTokenPages();
      showToast(t('settings.tokens.requestLimitReset'), 'success');
    } catch (error) {
      console.error('Failed to reset request limit', error);
      showToast(t('common.fail'), 'error');
    } finally {
      setResettingRequestLimitId(null);
    }
  };

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopyFeedbackId(id);
    setTimeout(() => setCopyFeedbackId(null), 2000);
  };

  const editingToken = quotaEditId
    ? [...activeTokens, ...disabledTokens].find((token) => token.id === quotaEditId) || null
    : null;

  return (
    <div className="space-y-8">
      <SlideInItem>
        <div>
          <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white tracking-tight">{t('settings.tokens.title')}</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-2 text-lg">{t('settings.securityDesc')}</p>
        </div>
      </SlideInItem>

      {/* Create Token */}
      <SlideInItem index={1}>
        {createdToken ? (
          <div className="p-6 bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-100 dark:border-emerald-800/50 rounded-2xl">
            <div className="flex items-start gap-4">
              <CheckCircleIcon className="text-emerald-600 dark:text-emerald-500 mt-1 flex-shrink-0 w-6 h-6" />
              <div className="flex-1 min-w-0">
                <h3 className="text-base font-bold text-emerald-800 dark:text-emerald-400 mb-1">{t('settings.tokens.generatedSuccess')}</h3>
                <p className="text-sm text-emerald-700 dark:text-emerald-500 mb-4">{t('settings.tokens.copyWarning')}</p>
                <div className="flex items-center gap-2">
                  <code className="flex-1 bg-white dark:bg-black border border-emerald-200 dark:border-emerald-800/50 px-4 py-3 rounded-xl font-mono text-sm text-gray-800 dark:text-gray-200 break-all shadow-sm">
                    {createdToken.token}
                  </code>
                  <Button
                    onClick={() => copyToClipboard(createdToken.token || '', 'new')}
                    variant="secondary"
                    className="w-12 px-0 bg-white dark:bg-black border border-emerald-200 dark:border-emerald-800/50 text-emerald-600 dark:text-emerald-500 hover:bg-emerald-50 dark:hover:bg-emerald-900/30"
                  >
                    {copyFeedbackId === 'new' ? <Check size={20} /> : <Copy size={20} />}
                  </Button>
                </div>
              </div>
            </div>
            <div className="pl-10 mt-3">
              <button onClick={() => setCreatedToken(null)} className="text-sm font-semibold text-emerald-700 dark:text-emerald-400 hover:text-emerald-900 dark:hover:text-emerald-300 underline underline-offset-2">
                {t('settings.tokens.generateAnother')}
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleCreateToken} className="p-6 bg-white dark:bg-[#1a1a1a] rounded-2xl border border-gray-200 dark:border-gray-800 shadow-card">
            <h3 className="text-sm font-bold text-gray-800 dark:text-gray-200 mb-4 uppercase tracking-wide">{t('settings.tokens.create')}</h3>
            <div className="flex flex-col sm:flex-row gap-3">
              <input
                type="text"
                value={newTokenName}
                onChange={(e) => setNewTokenName(e.target.value)}
                placeholder={t('settings.tokens.name') + ' (e.g. "Production App")'}
                className="flex-1 rounded-xl border-gray-200 dark:border-gray-700 text-sm focus:ring-black dark:focus:ring-white focus:border-black dark:focus:border-white py-2.5 px-4 border bg-gray-50 dark:bg-gray-900 dark:text-white transition-shadow"
                required
              />
              <input
                type="text"
                value={newTokenGroup}
                onChange={(e) => setNewTokenGroup(e.target.value)}
                placeholder={t('settings.tokens.group')}
                list="api-token-groups"
                className="w-full sm:w-48 rounded-xl border-gray-200 dark:border-gray-700 text-sm focus:ring-black dark:focus:ring-white focus:border-black dark:focus:border-white py-2.5 px-4 border bg-gray-50 dark:bg-gray-900 dark:text-white transition-shadow"
              />
              <datalist id="api-token-groups">
                {keyGroups.map((group) => (
                  <option key={group} value={group} />
                ))}
              </datalist>
              <Button type="submit" disabled={isCreatingToken || !newTokenName.trim()} variant="primary" leftIcon={isCreatingToken ? <Loader2 size={18} className="animate-spin" /> : <Plus size={18} />}>
                {t('common.add')}
              </Button>
            </div>
          </form>
        )}
      </SlideInItem>

      {/* Token List */}
      <SlideInItem index={2}>
        <div>
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h3 className="text-sm font-bold text-gray-900 dark:text-white uppercase tracking-wide">{t('settings.tokens.activeTokens')}</h3>
            <select
              value={selectedGroup}
              onChange={(event) => {
                setSelectedGroup(event.target.value);
                setActivePage(1);
                setDisabledPage(1);
              }}
              className="h-9 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 outline-none focus:ring-2 focus:ring-gray-900/10 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-200 dark:focus:ring-white/10"
            >
              {keyGroups.map((group) => (
                <option key={group} value={group}>{group}</option>
              ))}
            </select>
            <input
              type="search"
              value={keyword}
              onChange={(event) => {
                setKeyword(event.target.value);
                setActivePage(1);
                setDisabledPage(1);
              }}
              placeholder={t('settings.tokens.searchPlaceholder')}
              className="h-9 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 outline-none focus:ring-2 focus:ring-gray-900/10 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-200 dark:focus:ring-white/10 sm:w-72"
            />
          </div>
          {isActiveLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="w-8 h-8 text-gray-600 animate-spin" />
            </div>
          ) : activeTokens.length === 0 ? (
            <div className="text-center py-12 text-gray-500 dark:text-gray-400 bg-white dark:bg-[#1a1a1a] border border-dashed border-gray-200 dark:border-gray-800 rounded-2xl">
              {t('settings.tokens.empty')}
            </div>
          ) : (
            <div className="space-y-3">
              {activeTokens.map(token => (
                <TokenCard
                  key={token.id}
                  token={token}
                  revokeId={revokeId}
                  copyFeedbackId={copyFeedbackId}
                  onCopy={copyToClipboard}
                  onToggle={handleToggleToken}
                  onRevokeClick={setRevokeId}
                  onRevokeConfirm={handleRevokeToken}
                  onRevokeCancel={() => setRevokeId(null)}
                  resettingRequestLimitId={resettingRequestLimitId}
                  onQuotaEdit={startQuotaEdit}
                  onResetRequestLimitClick={setResetConfirmToken}
                  t={t}
                />
              ))}
              {activeTotal > PAGE_SIZE && (
                <Pagination
                  current={activePage}
                  size={PAGE_SIZE}
                  total={activeTotal}
                  onChange={(page) => setActivePage(page)}
                />
              )}
            </div>
          )}
        </div>
      </SlideInItem>

      {/* Disabled Tokens */}
      {(disabledTotal > 0 || showDisabled) && (
        <SlideInItem index={3}>
          <div>
            <button
              onClick={() => setShowDisabled(!showDisabled)}
              className="flex items-center gap-2 text-sm font-bold text-gray-500 dark:text-gray-400 mb-4 uppercase tracking-wide hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
            >
              {showDisabled ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
              {t('settings.tokens.disabledTokens')} ({disabledTotal})
            </button>
            {showDisabled && (
              isDisabledLoading ? (
                <div className="flex justify-center py-8">
                  <Loader2 className="w-7 h-7 text-gray-600 animate-spin" />
                </div>
              ) : (
                <div className="space-y-3">
                  {disabledTokens.map(token => (
                    <TokenCard
                      key={token.id}
                      token={token}
                      revokeId={revokeId}
                      copyFeedbackId={copyFeedbackId}
                      onCopy={copyToClipboard}
                      onToggle={handleToggleToken}
                      onRevokeClick={setRevokeId}
                      onRevokeConfirm={handleRevokeToken}
                      onRevokeCancel={() => setRevokeId(null)}
                      resettingRequestLimitId={resettingRequestLimitId}
                      onQuotaEdit={startQuotaEdit}
                      onResetRequestLimitClick={setResetConfirmToken}
                      t={t}
                    />
                  ))}
                  {disabledTotal > PAGE_SIZE && (
                    <Pagination
                      current={disabledPage}
                      size={PAGE_SIZE}
                      total={disabledTotal}
                      onChange={(page) => setDisabledPage(page)}
                    />
                  )}
                </div>
              )
            )}
          </div>
        </SlideInItem>
      )}
      <Modal
        isOpen={!!editingToken}
        onClose={cancelQuotaEdit}
        title={editingToken ? `${t('settings.tokens.editSettings')}: ${editingToken.name}` : t('settings.tokens.editSettings')}
        size="lg"
        footer={
          <>
            <Button type="button" onClick={cancelQuotaEdit} variant="secondary" leftIcon={<X size={15} />}>
              {t('common.cancel')}
            </Button>
            <Button
              type="button"
              onClick={() => quotaEditId && handleQuotaSave(quotaEditId)}
              variant="primary"
              loading={savingQuotaId === quotaEditId}
              leftIcon={<Check size={15} />}
            >
              {t('common.save')}
            </Button>
          </>
        }
      >
        {editingToken && (
          <div className="space-y-5">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <label className="space-y-1.5">
                <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{t('settings.tokens.quota')}</span>
                <input
                  type="number"
                  min="0"
                  step="0.0001"
                  value={quotaInputs[editingToken.id] || ''}
                  onChange={(event) => handleQuotaInputChange(editingToken.id, event.target.value)}
                  placeholder={t('settings.tokens.unlimited')}
                  className="h-10 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 outline-none focus:ring-2 focus:ring-gray-900/10 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-200 dark:focus:ring-white/10"
                />
              </label>
              <label className="space-y-1.5">
                <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{t('settings.tokens.requestLimit')}</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={requestLimitInputs[editingToken.id] || ''}
                  onChange={(event) => handleRequestLimitInputChange(editingToken.id, event.target.value)}
                  placeholder={t('settings.tokens.unlimited')}
                  className="h-10 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 outline-none focus:ring-2 focus:ring-gray-900/10 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-200 dark:focus:ring-white/10"
                />
              </label>
              <label className="space-y-1.5 sm:col-span-2">
                <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{t('settings.tokens.concurrencyLimit')}</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={concurrencyLimitInputs[editingToken.id] || ''}
                  onChange={(event) => handleConcurrencyLimitInputChange(editingToken.id, event.target.value)}
                  placeholder={t('settings.tokens.unlimited')}
                  className="h-10 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 outline-none focus:ring-2 focus:ring-gray-900/10 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-200 dark:focus:ring-white/10"
                />
              </label>
              <label className="space-y-1.5 sm:col-span-2">
                <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{t('settings.tokens.group')}</span>
                <input
                  type="text"
                  value={groupInputs[editingToken.id] || ''}
                  onChange={(event) => handleGroupInputChange(editingToken.id, event.target.value)}
                  list={`api-token-modal-groups-${editingToken.id}`}
                  placeholder={DEFAULT_KEY_GROUP}
                  className="h-10 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 outline-none focus:ring-2 focus:ring-gray-900/10 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-200 dark:focus:ring-white/10"
                />
                <datalist id={`api-token-modal-groups-${editingToken.id}`}>
                  {keyGroups.map((group) => (
                    <option key={group} value={group} />
                  ))}
                </datalist>
              </label>
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between gap-2">
                <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{t('settings.tokens.modelLimit')}</span>
                <button
                  type="button"
                  onClick={() => handleModelLimitClear(editingToken.id)}
                  className="text-xs font-medium text-gray-500 hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-200"
                >
                  {t('settings.tokens.allModels')}
                </button>
              </div>
              <div className="max-h-56 overflow-y-auto rounded-lg border border-gray-200 bg-white p-3 dark:border-gray-700 dark:bg-gray-950">
                {modelOptions.length === 0 ? (
                  <div className="text-sm text-gray-500 dark:text-gray-400">{t('settings.tokens.noModelGroups')}</div>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {modelOptions.map((group) => {
                      const checked = (modelLimitInputs[editingToken.id] || []).includes(group.name);
                      return (
                        <button
                          key={group.id}
                          type="button"
                          onClick={() => handleModelLimitToggle(editingToken.id, group.name)}
                          className={`rounded-md border px-2.5 py-1.5 text-xs font-medium transition-colors ${
                            checked
                              ? 'border-gray-900 bg-gray-900 text-white dark:border-white dark:bg-white dark:text-gray-950'
                              : 'border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:border-gray-600'
                          }`}
                        >
                          {group.name}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </Modal>
      <Modal
        isOpen={!!resetConfirmToken}
        onClose={() => setResetConfirmToken(null)}
        title={t('settings.tokens.resetRequestLimit')}
        size="sm"
        footer={
          <>
            <Button type="button" onClick={() => setResetConfirmToken(null)} variant="secondary" leftIcon={<X size={15} />}>
              {t('common.cancel')}
            </Button>
            <Button
              type="button"
              onClick={() => resetConfirmToken && handleResetRequestLimit(resetConfirmToken.id)}
              variant="danger"
              loading={resettingRequestLimitId === resetConfirmToken?.id}
              leftIcon={<RotateCcw size={15} />}
            >
              {t('settings.tokens.resetRequestLimitConfirmAction')}
            </Button>
          </>
        }
      >
        {resetConfirmToken && (
          <div className="space-y-3 text-sm text-gray-600 dark:text-gray-300">
            <p>{t('settings.tokens.resetRequestLimitConfirm')}</p>
            <div className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 dark:border-gray-700 dark:bg-gray-900">
              <div className="font-semibold text-gray-900 dark:text-gray-100">{resetConfirmToken.name}</div>
              <div className="mt-1 font-mono text-xs text-gray-500 dark:text-gray-400">{resetConfirmToken.maskedToken}</div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
interface TokenCardProps {
  token: AccessToken;
  revokeId: string | null;
  copyFeedbackId: string | null;
  onCopy: (text: string, id: string) => void;
  onToggle: (id: string) => void;
  onRevokeClick: (id: string) => void;
  onRevokeConfirm: (id: string) => void;
  onRevokeCancel: () => void;
  resettingRequestLimitId: string | null;
  onQuotaEdit: (token: AccessToken) => void;
  onResetRequestLimitClick: (token: AccessToken) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
}

const TokenCard: React.FC<TokenCardProps> = ({
  token,
  revokeId,
  copyFeedbackId,
  onCopy,
  onToggle,
  onRevokeClick,
  onRevokeConfirm,
  onRevokeCancel,
  resettingRequestLimitId,
  onQuotaEdit,
  onResetRequestLimitClick,
  t,
}) => (
  <div className="p-4 bg-white dark:bg-[#1a1a1a] border border-gray-200 dark:border-gray-800 rounded-xl hover:border-gray-300 dark:hover:border-gray-600 hover:shadow-sm transition-all">
    <div className="flex flex-col sm:flex-row sm:items-center justify-between">
      <div className="mb-3 sm:mb-0">
        <div className="flex items-center gap-3 flex-wrap">
          <span className="font-bold text-gray-800 dark:text-gray-200">{token.name}</span>
          <span className="text-xs font-semibold bg-gray-100 dark:bg-gray-900 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded border border-gray-200 dark:border-gray-700 whitespace-nowrap">
            {token.keyGroup || DEFAULT_KEY_GROUP}
          </span>
          <span className="text-xs font-mono bg-gray-100 dark:bg-gray-900 text-gray-500 dark:text-gray-400 px-2 py-0.5 rounded border border-gray-200 dark:border-gray-700 whitespace-nowrap">
            {token.maskedToken}
          </span>
          <Button onClick={() => onCopy(token.token || '', token.id)} variant="ghost" className="px-2" title={t('common.copy')}>
            {copyFeedbackId === token.id ? <Check size={16} className="text-green-600" /> : <Copy size={16} />}
          </Button>
          {token.expiredAt && token.expiredAt > 0 && (
            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
              Date.now() / 1000 > token.expiredAt
                ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400'
                : 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400'
            }`}>
              {Date.now() / 1000 > token.expiredAt
                ? t('settings.tokens.expired')
                : `${t('settings.tokens.expires')}: ${new Date(token.expiredAt * 1000).toLocaleDateString()}`
              }
            </span>
          )}
        </div>
      </div>

      <div className="flex items-center gap-1 mt-2 sm:mt-0">
        {revokeId === token.id ? (
          <div className="flex items-center gap-2 bg-red-50 dark:bg-red-900/20 p-2 rounded-xl border border-red-100 dark:border-red-900/50">
            <span className="text-xs text-red-700 dark:text-red-400 font-bold px-1">{t('settings.tokens.confirmRevokeShort')}</span>
            <Button onClick={() => onRevokeConfirm(token.id)} variant="secondary" size="sm" className="px-2 bg-white dark:bg-gray-800 text-red-600 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/30">
              <Check size={16} />
            </Button>
            <Button onClick={onRevokeCancel} variant="secondary" size="sm" className="px-2 bg-white dark:bg-gray-800 text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
              <X size={16} />
            </Button>
          </div>
        ) : (
          <>
            <Button
              onClick={() => onQuotaEdit(token)}
              variant="secondary"
              size="sm"
              className="h-9 shrink-0 rounded-lg px-3 text-xs text-gray-600 shadow-none dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800"
              title={t('settings.tokens.editSettings')}
              leftIcon={<PencilLine size={14} />}
            >
              {t('settings.tokens.setQuota')}
            </Button>
            <Button
              onClick={() => onToggle(token.id)}
              variant="ghost"
              className={`px-2 ${token.status === 'active' ? 'text-green-600 hover:text-orange-600 hover:bg-orange-50 dark:hover:bg-orange-900/20' : 'text-gray-400 hover:text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20'}`}
              title={token.status === 'active' ? t('settings.tokens.disable') : t('settings.tokens.enable')}
            >
              {token.status === 'active' ? <ToggleRight size={22} /> : <ToggleLeft size={22} />}
            </Button>
            <Button onClick={() => onRevokeClick(token.id)} variant="ghost" className="px-2 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20" title={t('common.delete')}>
              <Trash2 size={20} />
            </Button>
            <Button
              onClick={() => onResetRequestLimitClick(token)}
              variant="ghost"
              className="px-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20"
              title={t('settings.tokens.resetRequestLimit')}
              disabled={resettingRequestLimitId === token.id}
            >
              {resettingRequestLimitId === token.id ? <Loader2 size={18} className="animate-spin" /> : <RotateCcw size={18} />}
            </Button>
          </>
        )}
      </div>
    </div>

    {/* Usage Stats */}
    <div className="mt-3 pt-3 border-t border-gray-100 dark:border-gray-800 grid grid-cols-2 lg:grid-cols-8 gap-3">
      <div className="flex items-center gap-1.5">
        <BarChart3 size={13} className="text-gray-400" />
        <span className="text-xs text-gray-500 dark:text-gray-400">{t('settings.tokens.requests')}:</span>
        <span className="text-xs font-semibold text-gray-700 dark:text-gray-300">{(token.totalRequests || 0).toLocaleString()}</span>
      </div>
      <div className="flex items-center gap-1.5">
        <span className="text-xs text-gray-500 dark:text-gray-400">{t('settings.tokens.inputTokens')}:</span>
        <span className="text-xs font-semibold text-gray-700 dark:text-gray-300">{formatTokenCount(token.totalInputTokens || 0)}</span>
      </div>
      <div className="flex items-center gap-1.5">
        <span className="text-xs text-gray-500 dark:text-gray-400">{t('settings.tokens.outputTokens')}:</span>
        <span className="text-xs font-semibold text-gray-700 dark:text-gray-300">{formatTokenCount(token.totalOutputTokens || 0)}</span>
      </div>
      <div className="flex items-center gap-1.5">
        <span className="text-xs text-gray-500 dark:text-gray-400">{t('settings.tokens.cost')}:</span>
        <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">${(token.totalCost || 0).toFixed(4)}</span>
      </div>
      <div className="flex items-center gap-1.5 min-w-0 col-span-2 lg:col-span-1">
        <span className="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">{t('settings.tokens.quota')}:</span>
        <span className={`text-xs font-semibold truncate ${isQuotaExceeded(token) ? 'text-red-600 dark:text-red-400' : 'text-gray-700 dark:text-gray-300'}`}>
          {formatQuota(token, t)}
        </span>
      </div>
      <div className="flex items-center gap-1.5 min-w-0 col-span-2 lg:col-span-1">
        <span className="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">{t('settings.tokens.requestLimit')}:</span>
        <span className={`text-xs font-semibold truncate ${isRequestLimitExceeded(token) ? 'text-red-600 dark:text-red-400' : 'text-gray-700 dark:text-gray-300'}`}>
          {formatRequestLimit(token, t)}
        </span>
      </div>
      <div className="flex items-center gap-1.5 min-w-0 col-span-2 lg:col-span-1">
        <span className="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">{t('settings.tokens.concurrencyLimit')}:</span>
        <span className="text-xs font-semibold truncate text-gray-700 dark:text-gray-300">
          {formatConcurrencyLimit(token, t)}
        </span>
      </div>
      <div className="flex items-center gap-1.5 min-w-0 col-span-2 lg:col-span-1">
        <span className="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">{t('settings.tokens.modelLimit')}:</span>
        <span className="text-xs font-semibold truncate text-gray-700 dark:text-gray-300">
          {formatModelLimit(token, t)}
        </span>
      </div>
    </div>
  </div>
);

const formatTokenCount = (count: number): string => {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${(count / 1_000).toFixed(1)}K`;
  return String(count);
};

const formatQuota = (token: AccessToken, t: (key: string, params?: Record<string, string | number>) => string): string => {
  if (token.maxAmount == null) {
    return t('settings.tokens.unlimited');
  }
  return `$${(token.totalCost || 0).toFixed(4)} / $${token.maxAmount.toFixed(4)}`;
};

const formatRequestLimit = (token: AccessToken, t: (key: string, params?: Record<string, string | number>) => string): string => {
  if (token.maxRequests == null) {
    return t('settings.tokens.unlimited');
  }
  return `${(token.requestLimitUsed || 0).toLocaleString()} / ${token.maxRequests.toLocaleString()}`;
};

const formatConcurrencyLimit = (token: AccessToken, t: (key: string, params?: Record<string, string | number>) => string): string => {
  if (token.maxConcurrentRequests == null) {
    return t('settings.tokens.unlimited');
  }
  return token.maxConcurrentRequests.toLocaleString();
};

const formatModelLimit = (token: AccessToken, t: (key: string, params?: Record<string, string | number>) => string): string => {
  if (!token.supportedModels || token.supportedModels.length === 0) {
    return t('settings.tokens.allModels');
  }
  if (token.supportedModels.length <= 2) {
    return token.supportedModels.join(', ');
  }
  return t('settings.tokens.modelLimitSummary', { count: token.supportedModels.length });
};

const isQuotaExceeded = (token: AccessToken): boolean => {
  return token.maxAmount != null && (token.totalCost || 0) >= token.maxAmount;
};

const isRequestLimitExceeded = (token: AccessToken): boolean => {
  return token.maxRequests != null && (token.requestLimitUsed || 0) >= token.maxRequests;
};

const CheckCircleIcon = ({ className }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
    <polyline points="22 4 12 14.01 9 11.01"></polyline>
  </svg>
);
