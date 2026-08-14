import { useEffect, useState } from 'react';
import { api } from './services/api.js';
import Header from './components/Header.jsx';
import ActiveListCard from './components/ActiveListCard.jsx';
import InsightCard from './components/InsightCard.jsx';
import PlanCard from './components/PlanCard.jsx';
import TopicCard from './components/TopicCard.jsx';
import RecommendationCard from './components/RecommendationCard.jsx';
import MistakeCard from './components/MistakeCard.jsx';
import LeetCodeSettingsModal from './components/modals/LeetCodeSettingsModal.jsx';
import LlmSettingsModal from './components/modals/LlmSettingsModal.jsx';
import MistakeNoteModal from './components/modals/MistakeNoteModal.jsx';
import ExplainModal from './components/modals/ExplainModal.jsx';
import TopicsModal from './components/modals/TopicsModal.jsx';

export default function App() {
  const [lists, setLists] = useState([]);
  const [active, setActive] = useState(null);
  const [plan, setPlan] = useState(null);
  const [checkin, setCheckin] = useState(null);
  const [stats, setStats] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [mistakes, setMistakes] = useState([]);
  const [topics, setTopics] = useState([]);
  const [problemTitles, setProblemTitles] = useState({});
  const [insight, setInsight] = useState('');
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [switching, setSwitching] = useState(false);
  const [savingSettings, setSavingSettings] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showSettings, setShowSettings] = useState(false);
  const [leetcodeSettings, setLeetCodeSettings] = useState(null);
  const [sessionInput, setSessionInput] = useState('');
  const [csrfInput, setCsrfInput] = useState('');
  const [cfInput, setCfInput] = useState('');
  const [showLlmSettings, setShowLlmSettings] = useState(false);
  const [llmSettings, setLlmSettings] = useState(null);
  const [llmApiKey, setLlmApiKey] = useState('');
  const [llmBaseUrl, setLlmBaseUrl] = useState('https://api.deepseek.com/v1');
  const [llmModel, setLlmModel] = useState('deepseek-v4-flash');
  const [explainSlug, setExplainSlug] = useState(null);
  const [explainLevel, setExplainLevel] = useState(0);
  const [explainText, setExplainText] = useState('');
  const [explaining, setExplaining] = useState(false);
  const [mistakeSlug, setMistakeSlug] = useState(null);
  const [mistakeErrorType, setMistakeErrorType] = useState('');
  const [mistakeStuckPoint, setMistakeStuckPoint] = useState('');
  const [mistakeLesson, setMistakeLesson] = useState('');
  const [mistakeSimilar, setMistakeSimilar] = useState('');
  const [savingMistake, setSavingMistake] = useState(false);
  const [showTopics, setShowTopics] = useState(false);
  const [selectedTopicId, setSelectedTopicId] = useState(null);
  const [topicProblems, setTopicProblems] = useState([]);
  const [loadingTopic, setLoadingTopic] = useState(false);

  const busy = loading || syncing || switching || savingSettings;
  const weakTopics = stats.filter((stat) => stat.weak);
  const displayTitle = (slug) => problemTitles[slug] || slug;

  const loadAll = async () => {
    setLoading(true);
    setError('');
    try {
      const [listData, activeData, planData, checkinData, statsData, recData, mistakeData, topicData, titleData, insightData] =
        await Promise.all([
          api.getLists(),
          api.getActiveList(),
          api.getPlan(),
          api.getPlanStatus(),
          api.getTopicStats(),
          api.getRecommendations(),
          api.getMistakes(),
          api.getTopics(),
          api.getProblemTitles(),
          api.getInsight()
        ]);
      setLists(listData);
      setActive(activeData);
      setPlan(planData);
      setCheckin(checkinData);
      setStats(statsData);
      setRecommendations(recData);
      setMistakes(mistakeData);
      setTopics(topicData);
      setProblemTitles(titleData ?? {});
      setInsight(insightData?.content ?? '');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
    loadSettings();
    loadLlmSettings();
  }, []);

  const loadSettings = async () => {
    try {
      setLeetCodeSettings(await api.getLeetCodeSettings());
    } catch {
      setLeetCodeSettings(null);
    }
  };

  const loadLlmSettings = async () => {
    try {
      const data = await api.getLlmSettings();
      setLlmSettings(data);
      if (data?.baseUrl) setLlmBaseUrl(data.baseUrl);
      if (data?.model) setLlmModel(data.model);
    } catch {
      setLlmSettings(null);
    }
  };

  const handleSync = async () => {
    setError('');
    setNotice('');
    setSyncing(true);
    try {
      const result = await api.sync();
      setNotice(result?.message ?? '同步完成');
      await loadAll();
    } catch (err) {
      setError(err.message);
    } finally {
      setSyncing(false);
    }
  };

  const handleSetActive = async (listId) => {
    if (!listId || listId === active?.listId) return;
    setError('');
    setNotice('');
    setSwitching(true);
    try {
      const updated = await api.setActiveList(listId);
      if (updated) setActive(updated);
      await loadAll();
      setNotice('已切换主线题单');
    } catch (err) {
      setError(err.message);
    } finally {
      setSwitching(false);
    }
  };

  const handleRefreshLists = async () => {
    setError('');
    try {
      await api.refreshLists();
      setNotice('题单已从 leetcode.cn 刷新');
      await loadAll();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleSaveSettings = async () => {
    setError('');
    setSavingSettings(true);
    try {
      const saved = await api.saveLeetCodeSettings({
        session: sessionInput,
        csrfToken: csrfInput,
        cfClearance: cfInput
      });
      setLeetCodeSettings(saved);
      setSessionInput('');
      setCsrfInput('');
      setCfInput('');
      setShowSettings(false);
      setNotice('LeetCode 登录态已保存，现在可以真实同步');
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingSettings(false);
    }
  };

  const handleSaveLlmSettings = async () => {
    setError('');
    setSavingSettings(true);
    try {
      const saved = await api.saveLlmSettings({
        apiKey: llmApiKey,
        baseUrl: llmBaseUrl,
        model: llmModel
      });
      setLlmSettings(saved);
      setLlmApiKey('');
      setShowLlmSettings(false);
      setNotice('AI 配置已保存，解析和周报将使用真实模型');
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingSettings(false);
    }
  };

  const handleExplain = async (level) => {
    setError('');
    setExplainLevel(level);
    setExplaining(true);
    try {
      const result = await api.explain(explainSlug, level);
      setExplainText(result.content ?? result);
    } catch (err) {
      setError(err.message);
    } finally {
      setExplaining(false);
    }
  };

  const openExplain = (slug) => {
    setExplainSlug(slug);
    setExplainText('');
    setExplainLevel(0);
  };

  const handleReviewMistake = async (slug, passed) => {
    setError('');
    try {
      await api.reviewMistake(slug, passed);
      setNotice(passed ? '已记录通过，题目进入下一轮复习' : '已记录未通过，明天继续复习');
      await loadAll();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleRefreshInsight = async () => {
    setError('');
    try {
      const result = await api.refreshInsight();
      setInsight(result?.content ?? '');
      setNotice('AI 周报已刷新');
    } catch (err) {
      setError(err.message);
    }
  };

  const handleCompletePlan = async () => {
    setError('');
    try {
      const status = await api.completePlan();
      setCheckin(status);
      setNotice(`今日打卡完成，已连续 ${status?.streak ?? 0} 天`);
    } catch (err) {
      setError(err.message);
    }
  };

  const openMistakeNote = (slug) => {
    const current = mistakes.find((item) => item.problemSlug === slug);
    setMistakeSlug(slug);
    setMistakeErrorType(current?.errorType ?? '');
    setMistakeStuckPoint(current?.stuckPoint ?? '');
    setMistakeLesson(current?.lesson ?? '');
    setMistakeSimilar(current?.similarProblems ?? '');
  };

  const handleSaveMistakeNote = async () => {
    setError('');
    setSavingMistake(true);
    try {
      await api.saveMistakeNote(mistakeSlug, {
        errorType: mistakeErrorType,
        stuckPoint: mistakeStuckPoint,
        lesson: mistakeLesson,
        similarProblems: mistakeSimilar
      });
      setMistakeSlug(null);
      setNotice('错题复盘已保存');
      await loadAll();
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingMistake(false);
    }
  };

  const openTopics = () => {
    setShowTopics(true);
    if (stats.length > 0) {
      selectTopic(stats[0].topicId);
    }
  };

  const selectTopic = async (topicId) => {
    setShowTopics(true);
    setSelectedTopicId(topicId);
    setLoadingTopic(true);
    try {
      setTopicProblems(await api.getTopicProblems(topicId));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingTopic(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      {busy && (
        <div className="fixed inset-x-0 top-0 z-50 h-1 overflow-hidden bg-indigo-100">
          <div className="h-full w-1/3 animate-pulse bg-indigo-500" />
        </div>
      )}

      <Header
        leetcodeSettings={leetcodeSettings}
        llmSettings={llmSettings}
        syncing={syncing}
        switching={switching}
        onOpenLeetCodeSettings={() => setShowSettings(true)}
        onOpenLlmSettings={() => setShowLlmSettings(true)}
        onSync={handleSync}
      />

      {error && (
        <div className="mb-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-red-700">{error}</div>
      )}
      {notice && (
        <div className="mb-6 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-emerald-700">{notice}</div>
      )}

      <ActiveListCard
        lists={lists}
        active={active}
        switching={switching}
        loading={loading}
        onSetActive={handleSetActive}
        onRefreshLists={handleRefreshLists}
      />

      <InsightCard insight={insight} onRefreshInsight={handleRefreshInsight} />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <PlanCard
          plan={plan}
          checkin={checkin}
          problemTitles={problemTitles}
          onCompletePlan={handleCompletePlan}
          onOpenExplain={openExplain}
        />
        <TopicCard
          weakTopics={weakTopics}
          topics={topics}
          onSelectTopic={selectTopic}
          onShowAll={openTopics}
        />
        <RecommendationCard recommendations={recommendations} problemTitles={problemTitles} />
        <MistakeCard
          mistakes={mistakes}
          problemTitles={problemTitles}
          onReviewMistake={handleReviewMistake}
          onOpenMistakeNote={openMistakeNote}
        />
      </div>

      {showSettings && (
        <LeetCodeSettingsModal
          sessionInput={sessionInput}
          csrfInput={csrfInput}
          cfInput={cfInput}
          onSessionChange={setSessionInput}
          onCsrfChange={setCsrfInput}
          onCfChange={setCfInput}
          onSave={handleSaveSettings}
          onClose={() => setShowSettings(false)}
          saving={savingSettings}
        />
      )}

      {showLlmSettings && (
        <LlmSettingsModal
          apiKey={llmApiKey}
          baseUrl={llmBaseUrl}
          model={llmModel}
          onApiKeyChange={setLlmApiKey}
          onBaseUrlChange={setLlmBaseUrl}
          onModelChange={setLlmModel}
          onSave={handleSaveLlmSettings}
          onClose={() => setShowLlmSettings(false)}
          saving={savingSettings}
        />
      )}

      {mistakeSlug && (
        <MistakeNoteModal
          errorType={mistakeErrorType}
          stuckPoint={mistakeStuckPoint}
          lesson={mistakeLesson}
          similarProblems={mistakeSimilar}
          onErrorTypeChange={setMistakeErrorType}
          onStuckPointChange={setMistakeStuckPoint}
          onLessonChange={setMistakeLesson}
          onSimilarChange={setMistakeSimilar}
          onSave={handleSaveMistakeNote}
          onClose={() => setMistakeSlug(null)}
          saving={savingMistake}
        />
      )}

      {explainSlug && (
        <ExplainModal
          slug={explainSlug}
          title={displayTitle(explainSlug)}
          level={explainLevel}
          text={explainText}
          explaining={explaining}
          onLevelChange={handleExplain}
          onClose={() => setExplainSlug(null)}
        />
      )}

      {showTopics && (
        <TopicsModal
          stats={stats}
          topics={topics}
          selectedTopicId={selectedTopicId}
          problems={loadingTopic ? [] : topicProblems}
          problemTitles={problemTitles}
          onSelectTopic={selectTopic}
          onClose={() => setShowTopics(false)}
        />
      )}
    </div>
  );
}
