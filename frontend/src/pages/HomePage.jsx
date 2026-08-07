import CtaBanner from '../components/home/CtaBanner'
import Hero from '../components/home/Hero'
import QuickAccess from '../components/home/QuickAccess'
import SiteFooter from '../components/home/SiteFooter'
import SiteHeader from '../components/home/SiteHeader'
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
