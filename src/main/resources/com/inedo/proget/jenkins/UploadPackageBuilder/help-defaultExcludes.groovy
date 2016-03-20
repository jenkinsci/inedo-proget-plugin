import org.apache.commons.lang.StringUtils

div {
    p {
        raw(_("p1"))
        br {}
        code {
            text(StringUtils.join(org.apache.tools.ant.DirectoryScanner.defaultExcludes, ', '))
        }
    }
    p {
        raw(_("p2"))
    }
}