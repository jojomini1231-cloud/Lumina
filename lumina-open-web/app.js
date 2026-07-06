const API_ORIGIN = window.LUMINA_PUBLIC_API_ORIGIN || "";
const API_BASE = `${API_ORIGIN.replace(/\/$/, "")}/api/v1`;

const form = document.querySelector("#queryForm");
const apiKeyInput = document.querySelector("#apiKey");
const toggleKey = document.querySelector("#toggleKey");
const toggleKeyIcon = document.querySelector("#toggleKeyIcon");
const submitButton = document.querySelector("#submitButton");
const formMessage = document.querySelector("#formMessage");
const results = document.querySelector("#results");
const keyName = document.querySelector("#keyName");
const statusPill = document.querySelector("#statusPill");
const availableRequests = document.querySelector("#availableRequests");
const usedRequests = document.querySelector("#usedRequests");
const maxRequests = document.querySelector("#maxRequests");
const successRequests = document.querySelector("#successRequests");
const modelScope = document.querySelector("#modelScope");
const modelGrid = document.querySelector("#modelGrid");

const statusText = {
  active: "可用",
  disabled: "已停用",
  expired: "已过期",
  exhausted: "次数已用完",
};

toggleKey.addEventListener("click", () => {
  const visible = apiKeyInput.type === "text";
  apiKeyInput.type = visible ? "password" : "text";
  toggleKeyIcon.textContent = visible ? "显示" : "隐藏";
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const apiKey = apiKeyInput.value.trim();
  if (!apiKey) {
    showMessage("请输入密钥", true);
    return;
  }

  setLoading(true);
  showMessage("正在查询密钥信息...", false);

  try {
    const [usage, models] = await Promise.all([
      fetchUsage(apiKey),
      fetchModels(apiKey),
    ]);

    renderResult(usage, models);
    showMessage("查询完成", false);
  } catch (error) {
    results.classList.add("hidden");
    showMessage(error.message || "查询失败，请稍后再试", true);
  } finally {
    setLoading(false);
  }
});

async function fetchUsage(apiKey) {
  const response = await fetch(`${API_BASE}/public/key-usage`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({ apiKey }),
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok || !payload || payload.code !== 200) {
    throw new Error(payload?.message || "查询失败，请稍后再试");
  }

  return payload.data;
}

async function fetchModels(apiKey) {
  const response = await fetch(`${API_ORIGIN.replace(/\/$/, "")}/v1/models`, {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${apiKey}`,
    },
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(payload?.error?.message || "模型列表查询失败");
  }

  return Array.isArray(payload?.data) ? payload.data : [];
}

function setLoading(loading) {
  submitButton.disabled = loading;
  submitButton.textContent = loading ? "查询中..." : "查询";
}

function showMessage(message, isError) {
  formMessage.textContent = message;
  formMessage.classList.toggle("error", isError);
}

function renderResult(data, models) {
  keyName.textContent = data.name || "未命名密钥";
  statusPill.textContent = statusText[data.status] || data.status || "-";
  statusPill.className = `status-pill ${data.status || ""}`;
  availableRequests.textContent = data.status === "active" && data.unlimitedRequests
    ? "无限制"
    : formatNumber(data.availableRequests);
  usedRequests.textContent = formatNumber(data.usedRequests);
  maxRequests.textContent = data.unlimitedRequests ? "无限制" : formatNumber(data.maxRequests);
  successRequests.textContent = formatNumber(data.successRequests);
  modelScope.textContent = data.modelScope === "all" ? "全部已启用模型" : "限定模型";

  renderModels(models);
  results.classList.remove("hidden");
}

function renderModels(models) {
  modelGrid.innerHTML = "";
  if (models.length === 0) {
    modelGrid.innerHTML = '<div class="empty">当前没有可展示的模型</div>';
    return;
  }

  const fragment = document.createDocumentFragment();
  models.forEach((model) => {
    const card = document.createElement("article");
    card.className = "model-card";

    const tags = buildTags(model);
    card.innerHTML = `
      <div>
        <div class="model-title">
          <h3>${escapeHtml(model.id || "未命名模型")}</h3>
          ${model.owned_by ? `<span class="provider">${escapeHtml(model.owned_by)}</span>` : ""}
        </div>
        <p class="model-name">${escapeHtml(model.id || "-")}</p>
        <div class="model-meta">
          <div><span>上下文</span><strong>${formatLimit(model.context_length)}</strong></div>
          <div><span>输出上限</span><strong>${formatLimit(model.max_completion_tokens)}</strong></div>
          <div><span>类型</span><strong>${escapeHtml(model.object || "model")}</strong></div>
          <div><span>创建时间</span><strong>${formatTimestamp(model.created)}</strong></div>
        </div>
      </div>
      <div class="tags">${tags.map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}</div>
    `;
    fragment.appendChild(card);
  });
  modelGrid.appendChild(fragment);
}

function buildTags(model) {
  const tags = [];
  if (model.owned_by) tags.push(model.owned_by);
  if (model.context_length) tags.push(`${formatLimit(model.context_length)} 上下文`);
  if (model.max_completion_tokens) tags.push(`${formatLimit(model.max_completion_tokens)} 输出`);
  return tags.length ? tags : ["标准模型"];
}

function formatNumber(value) {
  if (value === null || value === undefined) return "-";
  return new Intl.NumberFormat("zh-CN").format(Number(value));
}

function formatLimit(value) {
  if (!value) return "-";
  if (value >= 1000) return `${formatNumber(value / 1000)}K`;
  return formatNumber(value);
}

function formatTimestamp(value) {
  if (!value) return "-";
  const date = new Date(Number(value) * 1000);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleDateString("zh-CN");
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
