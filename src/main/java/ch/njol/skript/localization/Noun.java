/*
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Copyright 2011-2013 Peter Güttinger
 * 
 */

package ch.njol.skript.localization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.localization.Language.LanguageListenerPriority;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;

/**
 * @author Peter Güttinger
 */
public class Noun extends Message {
	
	public final static String GENDERS_SECTION = "genders.";
	
	public final static int PLURAL = -2, NO_GENDER = -3; // -1 is sometimes used as 'not set'
	public final static String PLURAL_TOKEN = "x", NO_GENDER_TOKEN = "-";
	
	private String singular, plural;
	private int gender = 0;
	
	public Noun(final String key) {
		super(key);
	}
	
	@Override
	protected void onValueChange() {
		String value = getValue();
		if (value == null) {
			plural = singular = key;
			gender = 0;
			return;
		}
		final int g = value.lastIndexOf('@');
		if (g != -1) {
			gender = getGender(value.substring(g + 1).trim(), key);
			value = value.substring(0, g).trim();
		} else {
			gender = 0;
		}
		final Pair<String, String> p = Noun.getPlural(value);
		singular = p.first;
		plural = p.second;
		if (gender == PLURAL && !singular.equals(plural))
			Skript.warning("Noun '" + key + "' is of gender 'plural', but has different singular and plural values.");
	}
	
	@Override
	public String toString() {
		validate();
		return singular;
	}
	
	public String toString(final boolean plural) {
		validate();
		return plural ? this.plural : singular;
	}
	
	public String withIndefiniteArticle() {
		return toString(Language.F_INDEFINITE_ARTICLE);
	}
	
	public String getIndefiniteArticle() {
		validate();
		return gender == PLURAL || gender == NO_GENDER ? "" : indefiniteArticles.get(gender);
	}
	
	public String withDefiniteArticle() {
		return toString(Language.F_DEFINITE_ARTICLE);
	}
	
	public String withDefiniteArticle(final boolean plural) {
		return toString(Language.F_DEFINITE_ARTICLE | (plural ? Language.F_PLURAL : 0));
	}
	
	public String getDefiniteArticle() {
		validate();
		return gender == PLURAL ? definitePluralArticle : gender == NO_GENDER ? "" : definiteArticles.get(gender);
	}
	
	public String toString(final int flags) {
		validate();
		final StringBuilder b = new StringBuilder();
		if (gender == PLURAL) {
			if ((flags & Language.F_DEFINITE_ARTICLE) != 0)
				b.append(definitePluralArticle).append(" ");
		} else if (gender == NO_GENDER) {
			// nothing
		} else if ((flags & Language.F_DEFINITE_ARTICLE) != 0) {
			b.append(definiteArticles.get(gender)).append(" ");;
		} else if ((flags & Language.F_INDEFINITE_ARTICLE) != 0) {
			b.append(indefiniteArticles.get(gender)).append(" ");;
		}
		b.append((flags & Language.F_PLURAL) != 0 ? plural : singular);
		return b.toString();
	}
	
	public String withAmount(final double amount) {
		validate();
		return Skript.toString(amount) + " " + (amount == 1 ? singular : plural);
	}
	
	public String withAmount(final double amount, final int flags) {
		validate();
		if (amount == 1) {
			if (gender == NO_GENDER)
				return toString((flags & Language.F_PLURAL) != 0);
			if (gender == PLURAL) {
				if ((flags & Language.F_DEFINITE_ARTICLE) != 0)
					return definitePluralArticle + " " + plural;
				return plural;
			}
			if ((flags & Language.F_DEFINITE_ARTICLE) != 0)
				return (flags & Language.F_PLURAL) != 0 ? definitePluralArticle + " " + plural : definiteArticles.get(gender) + " " + singular;
			if ((flags & Language.F_INDEFINITE_ARTICLE) != 0)
				return indefiniteArticles.get(gender) + " " + singular;
			if ((flags & Language.F_PLURAL) != 0)
				return plural;
		}
		return Skript.toString(amount) + " " + (amount == 1 ? singular : plural);
	}
	
	public String getSingular() {
		validate();
		return singular;
	}
	
	public String getPlural() {
		validate();
		return plural;
	}
	
	/**
	 * @param s String with ¦ plural markers but without a @gender
	 * @return (singular, plural)
	 */
	public static Pair<String, String> getPlural(final String s) {
		final Pair<String, String> r = new Pair<String, String>("", "");
		int part = 3; // 1 = singular, 2 = plural, 3 = both
		int i = StringUtils.count(s, '¦');
		int last = 0, c = -1;
		while ((c = s.indexOf('¦', c + 1)) != -1) {
			final String x = s.substring(last, c);
			if ((part & 1) != 0)
				r.first += x;
			if ((part & 2) != 0)
				r.second += x;
			part = i >= 2 ? (part % 3) + 1 : (part == 2 ? 3 : 2);
			last = c + 1;
			i--;
		}
		final String x = s.substring(last);
		if ((part & 1) != 0)
			r.first += x;
		if ((part & 2) != 0)
			r.second += x;
		return r;
	}
	
	/**
	 * Normalizes plural markers, i.e. increases the total number of markers to a multiple of 3 without changing the string's meaning.
	 * <p>
	 * A @gender at the end of the string will be treated correctly.
	 * 
	 * @param s Some string
	 * @return The same string with normalized plural markers
	 */
	public static String normalizePluralMarkers(final String s) {
		final int c = StringUtils.count(s, '¦');
		if (c % 3 == 0)
			return s;
		if (c % 3 == 2) {
			final int g = s.lastIndexOf('@');
			if (g == -1)
				return s + "¦";
			return s.substring(0, g) + "¦" + s.substring(g);
		}
		final int x = s.lastIndexOf('¦');
		final int g = s.lastIndexOf('@');
		if (g == -1)
			return s.substring(0, x) + "¦" + s.substring(x) + "¦";
		return s.substring(0, x) + "¦" + s.substring(x, g) + "¦" + s.substring(g);
	}
	
	private final static HashMap<String, Integer> genders = new HashMap<String, Integer>();
	
	/**
	 * @param gender Gender id as defined in [language].lang (without the leading @)
	 * @param key Key to use in error messages§
	 * @return
	 */
	private static int getGender(final String gender, final String key) {
		if (gender.equalsIgnoreCase(PLURAL_TOKEN))
			return PLURAL;
		if (gender.equalsIgnoreCase(NO_GENDER_TOKEN))
			return NO_GENDER;
		final Integer i = genders.get(gender);
		if (i != null)
			return i;
		Skript.warning("Undefined gender '" + gender + "' at " + key);
		return 0;
	}
	
	public final static String getGenderID(final int gender) {
		if (gender == PLURAL)
			return PLURAL_TOKEN;
		if (gender == NO_GENDER)
			return NO_GENDER_TOKEN;
		return (Language.useLocal && Language.localized != null ? Language.localized : Language.english).get("genders." + gender + ".id");
	}
	
	/**
	 * For use by {@link Aliases}
	 * 
	 * @param s String
	 * @param key Key to report in case of error
	 * @return (stripped string, gender or -1 if none)
	 */
	public final static Pair<String, Integer> stripGender(String s, final String key) {
		final int c = s.lastIndexOf('@');
		int g = -1;
		if (c != -1) {
			g = getGender(s.substring(c + 1).trim(), key);
			s = s.substring(0, c).trim();
		}
		return new Pair<String, Integer>(s, g);
	}
	
	private final static List<String> indefiniteArticles = new ArrayList<String>(3);
	private final static List<String> definiteArticles = new ArrayList<String>(3);
	private static String definitePluralArticle = "";
	static {
		Language.addListener(new LanguageChangeListener() {
			@Override
			public void onLanguageChange() {
				final Map<String, String> lang = Language.useLocal ? Language.localized : Language.english;
				genders.clear();
				indefiniteArticles.clear();
				definiteArticles.clear();
				for (int i = 0; i < 100; i++) {
					final String g = lang.get(GENDERS_SECTION + i + ".id");
					if (g == null)
						break;
					if (g.equalsIgnoreCase(PLURAL_TOKEN) || g.equalsIgnoreCase(NO_GENDER_TOKEN)) {
						Skript.error("gender #" + i + " uses a reserved character as ID, please use something different!");
						continue;
					}
					genders.put(g, i);
					final String ia = lang.get(GENDERS_SECTION + i + ".indefinite article");
					indefiniteArticles.add(ia == null ? "" : ia);
					final String da = lang.get(GENDERS_SECTION + i + ".definite article");
					definiteArticles.add(da == null ? "" : da);
				}
				if (genders.isEmpty()) {
					Skript.error("No genders defined in language file " + Language.getName() + ".lang!");
					indefiniteArticles.add("");
					definiteArticles.add("");
				}
				final String dpa = lang.get(GENDERS_SECTION+"plural.definite article");
				if (dpa == null)
					Skript.error("Missing entry '"+GENDERS_SECTION+"plural.definite article' in the "+Language.getName()+" language file!");
				definitePluralArticle = dpa == null ? "" : dpa;
			}
		}, LanguageListenerPriority.EARLIEST);
	}
	
	public final static String stripIndefiniteArticle(final String s) {
		for (final String a : indefiniteArticles) {
			if (StringUtils.startsWithIgnoreCase(s, a + " "))
				return s.substring(a.length() + 1);
		}
		return s;
	}
	
	public final static boolean isIndefiniteArticle(final String s) {
		return indefiniteArticles.contains(s.toLowerCase());
	}
	
	public final static boolean isDefiniteArticle(final String s) {
		return definiteArticles.contains(s.toLowerCase());
	}
	
	public final static String toString(final String singular, final String plural, int gender, final int flags) {
		if (gender == NO_GENDER)
			return (flags & Language.F_PLURAL) != 0 ? plural : singular;
		if (gender == PLURAL)
			return (flags & Language.F_DEFINITE_ARTICLE) != 0 ? definitePluralArticle + " " + plural : plural;
		if (gender < 0 || gender >= indefiniteArticles.size())
			gender = 0;
		return ((flags & Language.F_INDEFINITE_ARTICLE) != 0 ? indefiniteArticles.get(gender) : (flags & Language.F_DEFINITE_ARTICLE) != 0 ? definiteArticles.get(gender) : "") +
				((flags & Language.F_PLURAL) != 0 ? plural : singular);
	}
	
}
