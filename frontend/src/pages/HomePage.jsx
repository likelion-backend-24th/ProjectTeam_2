import SiteFooter from '../components/common/SiteFooter'
import SiteHeader from '../components/common/SiteHeader'
import CtaBanner from '../components/home/CtaBanner'
import Hero from '../components/home/Hero'
import QuickAccess from '../components/home/QuickAccess'
import StatsBar from '../components/home/StatsBar'
import WhySection from '../components/home/WhySection'

export default function HomePage() {
  return (
    <>
      <SiteHeader />
      <main>
        <Hero />
        <StatsBar />
        <QuickAccess />
        <WhySection />
        <CtaBanner />
      </main>
      <SiteFooter />
    </>
  )
}
