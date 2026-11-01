import { useEffect, useState } from "react";

const worlds = [
  { number: "01", name: "The Fake Factory", note: "Production line: exits, traps, and questionable signage." },
  { number: "02", name: "Gravity Gone Wrong", note: "The ceiling is a floor. The floor is on a break." },
  { number: "03", name: "The Haunted Office", note: "Your meeting invite has a moving agenda." },
  { number: "04", name: "Reality Glitch", note: "The rules are still here. They just changed." },
  { number: "05", name: "The Impossible Museum", note: "Please do not touch the exit. It touches back." },
];

const mechanics = [
  { title: "False certainty", body: "Fake exits, fake checkpoints, and buttons that have read the fine print." },
  { title: "Unstable ground", body: "Invisible bridges, disappearing floors, and platforms with a short memory." },
  { title: "Broken physics", body: "Reverse gravity, moving hazards, wind, ice, and rooms that refuse to sit still." },
];

export default function App() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const sections = document.querySelectorAll<HTMLElement>(".reveal");
    const observer = new IntersectionObserver(
      (entries) => entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      }),
      { threshold: 0.16 },
    );
    sections.forEach((section) => observer.observe(section));
    return () => observer.disconnect();
  }, []);

  async function copyBuildCommand() {
    try {
      await navigator.clipboard.writeText("./gradlew :app:assembleDebug");
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
    }
  }

  function closeMenu() {
    setMenuOpen(false);
  }

  return (
    <div className="site-shell">
      <header className="site-nav">
        <a className="wordmark" href="#top" aria-label="Trap Theory home" onClick={closeMenu}>
          <span className="wordmark__mark">T</span>
          <span>TRAP THEORY</span>
        </a>
        <button
          className="nav-toggle"
          type="button"
          aria-label={menuOpen ? "Close navigation" : "Open navigation"}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          <span />
          <span />
        </button>
        <nav className={menuOpen ? "site-links is-open" : "site-links"} aria-label="Main navigation">
          <a href="#worlds" onClick={closeMenu}>THE WORLDS</a>
          <a href="#mechanics" onClick={closeMenu}>THE TRICKS</a>
          <a href="#build" onClick={closeMenu}>BUILD ON GITHUB</a>
        </nav>
      </header>

      <main>
        <section className="hero" id="top">
          <div className="hero__image" aria-hidden="true">
            <img src="/trap-theory-hero.jpg" alt="" fetchPriority="high" />
          </div>
          <div className="hero__shade" aria-hidden="true" />
          <div className="hero__copy">
            <p className="hero__overline"><span className="live-dot" /> A NATIVE ANDROID PUZZLE PLATFORMER</p>
            <h1 className="hero__title"><span>TRAP</span><span>THEORY</span></h1>
            <p className="hero__tagline">Nothing is what it seems.</p>
            <p className="hero__description">Forty-five levels of suspicious floors, dishonest doors, and physics that have stopped returning your calls.</p>
            <div className="hero__actions">
              <a className="button button--gold" href="#build">GET THE ANDROID BUILD <span aria-hidden="true">↗</span></a>
              <a className="text-link" href="#worlds">Explore the worlds <span aria-hidden="true">↓</span></a>
            </div>
          </div>
          <div className="hero__coordinates" aria-hidden="true">OFFLINE BY DESIGN<br />KOTLIN / ANDROID CANVAS</div>
          <a className="hero__scroll" href="#worlds" aria-label="Scroll to the game worlds"><span /></a>
        </section>

        <section className="worlds section-pad reveal" id="worlds">
          <div className="section-heading">
            <p className="section-kicker">THE TOUR</p>
            <h2>Five worlds.<br /><em>Zero safe assumptions.</em></h2>
            <p className="section-intro">A 45-level campaign where every room has a plan, and it is probably not yours.</p>
          </div>
          <div className="world-list">
            {worlds.map((world) => (
              <article className="world-row" key={world.number}>
                <span className="world-row__number">{world.number}</span>
                <h3>{world.name}</h3>
                <p>{world.note}</p>
                <span className="world-row__arrow" aria-hidden="true">↗</span>
              </article>
            ))}
          </div>
        </section>

        <section className="mechanics section-pad reveal" id="mechanics">
          <div className="mechanics__intro">
            <p className="section-kicker">THE RULEBOOK</p>
            <h2>Trust is<br />a <em>game mechanic.</em></h2>
            <p className="section-intro">It is not a very reliable one.</p>
          </div>
          <div className="mechanic-list">
            {mechanics.map((item, index) => (
              <article className="mechanic-row" key={item.title}>
                <span className="mechanic-row__index">0{index + 1}</span>
                <div>
                  <h3>{item.title}</h3>
                  <p>{item.body}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="build section-pad reveal" id="build">
          <div className="build__copy">
            <p className="section-kicker">NO PC REQUIRED</p>
            <h2>Build it from<br /><em>your phone.</em></h2>
            <p className="section-intro">Push this repository to GitHub. Actions compiles the native APK and leaves it in the run artifacts.</p>
            <a className="button button--line" href="README.md">READ THE REPOSITORY GUIDE <span aria-hidden="true">↗</span></a>
          </div>
          <div className="build-terminal" aria-label="Gradle build command">
            <div className="build-terminal__top"><span /><span /><span /><p>TRAP-THEORY / ANDROID</p></div>
            <div className="build-terminal__body">
              <p className="terminal-muted"># local Android build</p>
              <p><span className="terminal-prompt">$</span> ./gradlew :app:assembleDebug</p>
              <p className="terminal-muted terminal-indent">JDK 17  /  Gradle 8.11.1  /  SDK 35</p>
              <button className="copy-command" type="button" onClick={copyBuildCommand}>
                {copied ? "COPIED TO CLIPBOARD" : "COPY BUILD COMMAND"}
                <span aria-hidden="true">{copied ? "OK" : "+"}</span>
              </button>
            </div>
          </div>
        </section>

        <section className="closing section-pad reveal">
          <p className="section-kicker">ONE LAST THING</p>
          <h2>The exit is<br /><em>right there.</em></h2>
          <a className="button button--gold" href="#top">START OVER <span aria-hidden="true">↑</span></a>
        </section>
      </main>

      <footer className="site-footer">
        <a className="wordmark" href="#top"><span className="wordmark__mark">T</span><span>TRAP THEORY</span></a>
        <p>Native Android. Offline. Deliberately untrustworthy.</p>
        <a href="README.md">PROJECT README <span aria-hidden="true">↗</span></a>
      </footer>
    </div>
  );
}
