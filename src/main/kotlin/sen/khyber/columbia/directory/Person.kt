@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package sen.khyber.columbia.directory

import org.jsoup.nodes.Document
import org.jsoup.select.Elements

// TODO process these more?
inline class UNI(val value: String)

inline class Address(val value: String)
inline class Email(val value: String)
inline class PhoneNumber(val value: String)

data class Home(
        val address: Address,
        val homeAddress: Address?,
        val onCampus: Boolean
)

data class Contact(
        val email: Email,
        val emailAlias: Email?,
        val phoneNumber: PhoneNumber?,
        val campusPhoneNumber: PhoneNumber?,
        val fax: PhoneNumber?
)

data class Name(
        val firstName: String,
        val middleName: String?,
        val lastName: String
) {
    
    companion object {
        
        fun parse(name: String): Name? {
            val names = name.trim().split(" ")
            val size = names.size
            return when (size) {
                0, 1 -> null
                2 -> Name(names[0], null, names[1])
                3 -> Name(names[0], names[1], names[2])
                else -> Name(names[0], names.subList(1, size - 1).joinToString(" "), names[size - 1])
            }
        }
        
    }
    
}

data class Person(
        val uni: UNI,
        val name: Name,
        val title: String,
        val department: String,
        val home: Home,
        val contact: Contact
) {
    
    companion object {
        
        val parse = ::parsePerson
        
    }
    
}

private fun parseName(table: Elements): Name? = table
        .select("th")
        .text()
        .let { Name.parse(it) }

private fun parseFields(table: Elements): Map<String, String> = table
        .select("td")
        .asSequence()
        .map { it.text() }
        .map { it.trim() }
        .filterNot { it.isEmpty() }
        .map { it.removeSuffix(":") }
        .chunked(2)
        .associateBy({ it[0].toLowerCase() }, { it[1] })

// TODO move to right file
fun <T, R> logIfNull(log: (T) -> Unit, t: T, f: (T) -> R?): R? =
        f(t) ?: run {
            log(t)
            null
        }

private fun parsePerson(doc: Document): Person? {
    val realUni = doc.location().substringAfterLast("=")
    
    val table = doc.select(".table_results_indiv")
            .select("tbody")
    
    val name = parseName(table) ?: return null
    
    val fields = parseFields(table)
    
    val uni = fields["uni"]
    if (uni != realUni) {
        return null
    }
    
    val title = fields["title"] ?: return null
    val department = fields["department"] ?: return null
    val address = fields["address"] ?: return null
    
    val homeAddress = fields["home addr"]
    val emailAlias = fields["email"]
    
    return Person(
            uni = UNI(uni),
            name = name,
            title = title,
            department = department,
            home = Home(
                    address = Address(address),
                    homeAddress = homeAddress?.let { Address(it) },
                    onCampus = true
            ),
            contact = Contact(
                    email = Email("$uni@columbia.edu"),
                    emailAlias = emailAlias?.let { Email(it) },
                    phoneNumber = null,
                    campusPhoneNumber = null,
                    fax = null
            )
    )
}