import React, { useState } from 'react';
import { Layout } from './components/Layout';
import { Dashboard } from './components/Dashboard';
import { Providers } from './components/Providers';
import { Groups } from './components/Groups';
import { Settings } from './components/Settings';
import { Logs } from './components/Logs';
import { Pricing } from './components/Pricing';
import { Login } from './components/Login';
import { AuthProvider, useAuth } from './components/AuthContext';
import { ViewState } from './types';
import { LanguageProvider, useLanguage } from './components/LanguageContext';
import { ThemeProvider } from './components/ThemeContext';
import { Loader2 } from 'lucide-react';

// Page transition wrapper component
const PageTransition: React.FC<{ children: React.ReactNode; key: string }> = ({ children, key }) => (
  <div
    key={key}
    className="animate-in fade-in slide-in-from-bottom duration-300 ease-out-smooth"
  >
    {children}
  </div>
);

const AppContent: React.FC = () => {
  const [currentView, setCurrentView] = useState<ViewState>('dashboard');
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="h-screen w-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="w-10 h-10 text-indigo-600 animate-spin" />
          <p className="text-slate-500 text-sm animate-pulse">Loading...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Login />;
  }

  const renderView = () => {
    switch (currentView) {
      case 'dashboard':
        return <PageTransition key="dashboard"><Dashboard /></PageTransition>;
      case 'providers':
        return <PageTransition key="providers"><Providers /></PageTransition>;
      case 'groups':
        return <PageTransition key="groups"><Groups /></PageTransition>;
      case 'pricing':
        return <PageTransition key="pricing"><Pricing /></PageTransition>;
      case 'logs':
        return <PageTransition key="logs"><Logs /></PageTransition>;
      case 'settings':
        return <PageTransition key="settings"><Settings /></PageTransition>;
      default:
        return <PageTransition key="dashboard"><Dashboard /></PageTransition>;
    }
  };

  return (
    <Layout currentView={currentView} onChangeView={setCurrentView}>
      {renderView()}
    </Layout>
  );
};

const App: React.FC = () => {
    return (
        <LanguageProvider>
          <ThemeProvider>
            <AuthProvider>
              <AppContent />
            </AuthProvider>
          </ThemeProvider>
        </LanguageProvider>
    );
};

export default App;